// GroupService.java

package com.moeim.group;

import com.moeim.category.CategoryRepository;
import com.moeim.chat.ChatRoom;
import com.moeim.chat.ChatRoomRepository;
import com.moeim.chat.ChatRoomService;
import com.moeim.review.userreview.UserReviewRepository;
import com.moeim.schedule.ScheduleRepository;
import com.moeim.user.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private final ScheduleRepository scheduleRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomService chatRoomService;
    private final CategoryRepository categoryRepository;
    private final UserReviewRepository userReviewRepository;

    // 카테고리로 그룹 조회 (페이징)
    public Page<Group> getGroupsByCategoryId(Long categoryId, int page) {
        Pageable pageable = PageRequest.of(page, 3, Sort.by("createdAt").descending());
        return groupRepository.findByCategory_Id(categoryId, pageable);
    }

    //  list 페이지용
    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    // 전체 소모임 갖고오기 (페이징)
    public Page<Group> getAllGroups(int page) {
        // 소모임은 보통 최신순 혹은 인기순
        Pageable pageable = PageRequest.of(page, 3, Sort.by("createdAt").descending());
        return groupRepository.findAll(pageable);
    }

    // view 페이지에서 사용할 필수 함수
    public Group findById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));
    }

    @Transactional
    public Group save(Group group) {
        return groupRepository.save(group);
    }

    //  선택적으로 추가 가능 - 신청 가능 여부 판단
    public boolean isFull(Long groupId) {
        Group group = findById(groupId);
        return group.getNowCount() >= group.getMaxCount();
    }

    @Transactional
    public Page<Group> getGroupPage(Long categoryId,
                                    String keyword,
                                    String sortKey,
                                    boolean hideJoined,
                                    Long userId,
                                    int page,
                                    int size) {

        Pageable pageable;

        //  정렬 처리
        switch (sortKey) {

            case "latest":
                pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
                break;

            case "deadline":
                // Repository 레벨에서 정렬하므로 pageable은 기본값만 사용
                pageable = PageRequest.of(page, size);
                return groupRepository.searchGroupsDeadline(
                        (categoryId == null || categoryId == 0) ? null : categoryId,
                        (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim(),
                        hideJoined ? userId : null,
                        pageable
                );

            default: // 인기순(nowCount DESC)
                pageable = PageRequest.of(page, size,
                        Sort.by(Sort.Direction.DESC, "nowCount")
                                .and(Sort.by(Sort.Direction.DESC, "id"))
                );
        }

        //  기존 searchGroups 실행
        return groupRepository.searchGroups(
                (categoryId == null || categoryId == 0) ? null : categoryId,
                (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim(),
                hideJoined ? userId : null,
                pageable
        );
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));
        userReviewRepository.setGroupNullByGroupId(group.getId()); // 유저리뷰의 그룹란 널로 처리
        ChatRoom chatRoom = group.getChatRoom();
        if (chatRoom != null) {
            group.setChatRoom(null);
            chatRoomService.deleteChatRoom(chatRoom.getId());
        }
        groupRepository.delete(group);
    }

    @Transactional
    public void increaseCount(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));

        // 최대인원 초과 방지
        if (group.getNowCount() < group.getMaxCount()) {
            group.setNowCount(group.getNowCount() + 1);
            groupRepository.save(group);
        }
    }

    @Transactional
    public void saveMessage(Long groupId, String message) {
        // 너 DB 구조에 맞춰서 저장 로직 여기에 작성하면 됨.
        System.out.println("가입 메시지: " + message);
    }


//    public void delete(Group group) {
//        userReviewRepository.setGroupNullByGroupId(group.getId()); // 유저리뷰의 그룹란 널로 처리
//        groupRepository.delete(group);
//    }

    //그룹채팅 나가기
    public void leaveGroup(Long groupId, User user) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));

        // 1) 그룹 멤버십 제거
        GroupUser gu = groupUserRepository
                .findByGroupAndUser(group, user)
                .orElseThrow(() -> new IllegalArgumentException("이 그룹의 멤버가 아닙니다."));

        groupUserRepository.delete(gu);

        // 인원수 감소 (0 이하로 내려가지 않게 방어)
        long now = group.getNowCount() - 1;
        group.setNowCount(Math.max(now, 0));

        // 2) 그룹 채팅방에서도 나가기
        chatRoomService.leaveGroupChatRoom(group, user);
    }

    // 메인 페이지용: 모집 중인 모임 일부만 가져오기 (비로그인)
    public List<Group> getRecruitingGroups(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return groupRepository.findRecruitingGroups(pageable);
    }

    // (로그인용) 가입한 모임 제외하고 가져오기
    public List<Group> getRecruitingGroupsExcludeUser(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return groupRepository.findRecruitingGroupsExcludeUser(userId, pageable);
    }

    // (로그인용) 관심사 기반 추천 모임 가져오기
    public List<Group> getRecommendedGroups(Long userId, Set<Integer> interestIds, int limit) {

        // 관심사가 없으면 일반 최신글 반환
        if (interestIds == null || interestIds.isEmpty()) {
            return getRecruitingGroupsExcludeUser(userId, limit);
        }

        // 관심사 ID 변환
        List<Long> categoryIds = interestIds.stream()
                .map(Long::valueOf)
                .toList();

        // 관심사 + 가입제외 조회
        Pageable pageable = PageRequest.of(0, limit);
        List<Group> groups = groupRepository.findRecommendedGroups(categoryIds, userId, pageable);

        // 결과가 없으면 일반 최신글 반환
        if (groups.isEmpty()) {
            return getRecruitingGroupsExcludeUser(userId, limit);
        }

        return groups;
    }

    //  통계: 모집 중인 모임 수
    public long getRecruitingGroupCount() {
        return groupRepository.countRecruitingGroups();
    }

    //  통계: 모임에 참여 중인 유저 수 (중복 제거)
    public long getActiveUserCount() {
        return groupUserRepository.countDistinctUsers();
    }

}

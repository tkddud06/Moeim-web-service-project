package com.moeim.group;

import com.moeim.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupJoinRequestService {

    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final GroupRepository groupRepository;

    /**
     * 신청 목록 조회
     */
    public List<GroupJoinRequest> findRequests(Long groupId) {
        return groupJoinRequestRepository.findByGroup_Id(groupId);
    }

    /**
     * 가입 신청 저장
     */
    public void saveJoinRequest(Long groupId, User user, String message) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalStateException("Group not found: " + groupId));

        GroupJoinRequest request = GroupJoinRequest.builder()
                .group(group)
                .user(user)
                .text(message)
                .build();

        groupJoinRequestRepository.save(request);
    }

    /**
     * 이미 신청했는지 체크 (빠졌던 기능)
     */
    public boolean existsRequest(Long groupId, User user) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalStateException("Group not found: " + groupId));

        return groupJoinRequestRepository.existsByGroupAndUser(group, user);
    }

    public boolean existsByGroupAndUser(Group group, User user) {
        return groupJoinRequestRepository.existsByGroupAndUser(group, user);
    }

    public GroupJoinRequest findById(Long requestId) {
        return groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("JoinRequest not found: " + requestId));
    }


    public void delete(Long requestId) {
        groupJoinRequestRepository.deleteById(requestId);
    }

    @Transactional
    public void deleteAllByGroup(Group group) {
        groupJoinRequestRepository.deleteAllByGroup(group);
    }


}

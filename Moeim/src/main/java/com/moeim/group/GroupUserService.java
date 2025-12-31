package com.moeim.group;

import com.moeim.global.enums.PositionType;
import com.moeim.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupUserService {
    private final GroupUserRepository groupUserRepository;

    public boolean isAdmin(Group group, User user) {
        GroupUser groupUser = groupUserRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new IllegalArgumentException("관계를 찾을 수 없습니다"));
        return (groupUser.getPosition() == PositionType.ADMIN) ? true:false;
    }

    // 유저가 그룹에 존재하는지 여부만 조회
    public boolean isUserExistsInGroup(Group group, User user) {
        return groupUserRepository.existsByGroupAndUser(group, user);
    }

    public Page<GroupUser> getJoinedGroups(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        return groupUserRepository.findAllByUserId(userId, pageable);
    }

}

package com.moeim.group;
import com.moeim.user.User;
import com.moeim.group.Group;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {

    List<GroupJoinRequest> findByGroup_Id(Long groupId);

    boolean existsByGroupAndUser(Group group, User user);

    void deleteAllByGroup(Group group);



}

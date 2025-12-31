// MainController.java

package com.moeim.global;

import com.moeim.group.GroupService;
import com.moeim.user.User;
import com.moeim.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final GroupService groupService;
    private final UserService userService;

    @GetMapping("/")
    public String main(Model model, HttpSession session) {

        User sessionUser = (User) session.getAttribute("user");

        if (sessionUser != null) { // 유저 로그인 중이면 관심사 기반 추천
            // 최신 정보 갖고오기
            User user = userService.getUserById(sessionUser.getId());
            // 관심사 기반 추천 서비스 호출
            model.addAttribute("recruitingGroups", groupService.getRecommendedGroups(user.getId(), user.getInterestCategoryIds(),10));

            // 화면에 개인 전용 추천인지 아닌지 보여주기
            model.addAttribute("isRecommended", true);
        }
        else { // 로그인 중 아니면 그냥
            model.addAttribute("recruitingGroups", groupService.getRecruitingGroups(10));
            model.addAttribute("isRecommended", false); // 화면에 개인 전용 추천인지 아닌지 보여주기
        }


        //  통계용 숫자
        long groupCount = groupService.getRecruitingGroupCount();
        long userCount = groupService.getActiveUserCount();

        model.addAttribute("groupCount", groupCount);
        model.addAttribute("userCount", userCount);

        return "main";
    }
}

package com.moeim.category;

import com.moeim.global.enums.CategoryType;
import com.moeim.group.Group;
import com.moeim.group.GroupService;
import com.moeim.post.Post;
import com.moeim.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@Controller
@RequiredArgsConstructor
@RequestMapping("/category")
public class CategoryController {
    private final CategoryService categoryService;
    private final PostService postService;
    private final GroupService groupService;

    // 카테고리 페이지
    @GetMapping("/{categoryId}")
    public String list(Model model,
                       @PathVariable("categoryId") Long categoryId,
                       @RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "keyword", defaultValue = "") String keyword,
                       @RequestParam(value = "searchType", defaultValue = "all") String searchType) {

        Category category = categoryService.getCategoryById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리 존재하지 않음"));

        model.addAttribute("category", category);

        // 카테고리 목록
        List<Category> categoryList = categoryService.getAllCategories();
        model.addAttribute("categoryList", categoryList);

        // 검색한 경우와 하지 않은 경우 구분하여 포스트 페이징 넘기기
        Page<Post> postPaging;
        if (keyword == null || keyword.isEmpty()) { // 검색하지 않은 경우
            if (categoryId == 0) {
                // 전체 게시글 조회
                postPaging = this.postService.getAllPosts(page - 1);
            } else {
                postPaging = this.postService.getPostsByCategoryId(categoryId, page - 1);
            }
        } else { // 검색한 경우
            postPaging = this.postService.search(categoryId, searchType, keyword, page - 1);
        }
        model.addAttribute("postPaging", postPaging);

        // 일반 카테고리인 경우, 그룹 페이징도 추가
        if (category.getType() == CategoryType.CATEGORY) {
            Page<Group> groupPaging;
            if (categoryId == 0) {
                // 전체 소모임 조회
                groupPaging = this.groupService.getAllGroups(0);
            } else {
                groupPaging = this.groupService.getGroupsByCategoryId(categoryId, 0);
            }
            model.addAttribute("groupPaging", groupPaging);
        }

        // 현재 페이지 번호, 키워드, 서치타입 추가
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("searchType", searchType);

        return "Category/post_list";
    }
}

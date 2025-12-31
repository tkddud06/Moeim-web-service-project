package com.moeim.post;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostForm {

    @NotEmpty(message = "제목은 필수 항목입니다.")
    @Size(max = 50, message = "제목은 50자를 넘을 수 없습니다.")
    private String title;

    @NotEmpty(message = "내용은 필수 항목입니다.")
    private String text;
}
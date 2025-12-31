package com.moeim.global;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageResizeUtil {

    private static final int TARGET_SIZE = 240; // 사용자 설정 크기

    /**
     * 이미지 바이트 배열을 TARGET_SIZE x TARGET_SIZE 픽셀로 조건부 리사이징하여 반환
     * 원본이 목표 크기보다 작으면 강제 확대 없이 원본 그대로 저장
     *
     * originalBytes 원본 이미지 바이트 배열
     * outputFormat  출력 포맷 (예: "png" 또는 "jpg")
     * 리사이징된 이미지 바이트 배열 리턴
     */
    public static byte[] resize(byte[] originalBytes, String outputFormat) throws IOException {

        if (originalBytes == null || originalBytes.length == 0) {
            return new byte[0];
        }

        int targetSize = TARGET_SIZE;

        try (ByteArrayInputStream bis = new ByteArrayInputStream(originalBytes);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            // 1. 원본 이미지 로드 (스트림 1차 사용)
            BufferedImage originalImage = javax.imageio.ImageIO.read(bis);

            // 2. 크기 측정 (이미 로드된 BufferedImage 객체 사용)
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 3. 조건부 처리 및 저장
            if (originalWidth < targetSize || originalHeight < targetSize) {
                // 원본이 목표 크기(240px)보다 작다면, 원본 그대로 저장 (강제 확대 방지)
                javax.imageio.ImageIO.write(originalImage, outputFormat, bos);
            } else {
                // 원본이 목표 크기보다 크다면, 240x240으로 축소
                Thumbnails.of(originalImage) // BufferedImage 객체를 직접 Thumbnails에 전달
                        .size(targetSize, targetSize)
                        .outputFormat(outputFormat)
                        .toOutputStream(bos);
            }

            return bos.toByteArray();

        } catch (IOException e) {
            throw new IOException("이미지 리사이징 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
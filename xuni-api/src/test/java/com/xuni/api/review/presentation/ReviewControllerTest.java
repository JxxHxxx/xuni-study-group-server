package com.xuni.api.review.presentation;

import com.xuni.api.ApiDocumentUtils;
import com.xuni.api.auth.application.SimpleMemberDetails;
import com.xuni.api.auth.application.jwt.JwtTokenProvider;
import com.xuni.api.review.application.ReviewService;
import com.xuni.api.support.ControllerCommon;
import com.xuni.api.support.JwtTestConfiguration;
import com.xuni.core.review.domain.Progress;
import com.xuni.api.review.dto.request.ReviewForm;
import com.xuni.api.review.dto.request.ReviewUpdateForm;
import com.xuni.api.review.dto.response.ReviewOneResponse;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;

import static com.xuni.api.review.dto.response.ReviewApiMessage.*;
import static java.nio.charset.StandardCharsets.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({ReviewControllerTestConfig.class, JwtTestConfiguration.class})
class ReviewControllerTest extends ControllerCommon {

    @Autowired
    JwtTokenProvider testJwtTokenProvider;
    @Autowired
    ReviewService reviewService;

    String studyProductId = "study-product-identifier";

    @Test
    void create_review_docs() throws Exception {
        SimpleMemberDetails memberDetails = new SimpleMemberDetails(1l, "xuni@naver.com", "유니");
        ReviewForm form = new ReviewForm(3, "ORM 기초를 쌓는데 정말 유익한 것 같아요", 50);

        ResultActions result = mockMvc.perform(post("/study-products/{study-product-id}/reviews", studyProductId)
                .header(AUTHORIZATION, testJwtTokenProvider.issue(memberDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)));

        result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(REVIEW_CREATE))

                .andDo(document("review/create",
                        requestHeaders(
                                headerWithName(AUTHORIZATION).description("인증 토큰")
                        ),
                        responseHeaders(
                                headerWithName(LOCATION).description("리소스 생성 위치")
                        ),
                        pathParameters(
                                parameterWithName("study-product-id").description("스터디 상품 식별자")
                        ),
                        requestFields(
                                fieldWithPath("rating").type(JsonFieldType.NUMBER).description("평점"),
                                fieldWithPath("comment").type(JsonFieldType.STRING).description("한 줄 평"),
                                fieldWithPath("progress").type(JsonFieldType.NUMBER).description("스터디 상품 진행률 [API 호출을 통해 불러온다]")
                        ),
                        responseFields(
                                fieldWithPath("status").type(JsonFieldType.NUMBER).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                        )
                ));
    }

    @Test
    void read_review_by_docs() throws Exception {
        ReviewOneResponse response1 = new ReviewOneResponse(
                1l,
                "ORM 기초를 배우는데 좋은 것 같습니다.",
                3, LocalDateTime.now(),
                10l,
                "유니",
                Progress.HALF,
                5);

        ReviewOneResponse response2 = new ReviewOneResponse(
                2l,
                "김영한 그는 JPA의 신이야",
                3, LocalDateTime.of(2023,5, 15, 10, 20),
                15l,
                "허니",
                Progress.ALMOST,
                0);

        List<ReviewOneResponse> responses = List.of(response1, response2);

        BDDMockito.given(reviewService.read(any())).willReturn(responses);

        ResultActions result = mockMvc.perform(get("/study-products/{study-product-id}/reviews", studyProductId)
                .contentType(MediaType.APPLICATION_JSON));

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(REVIEW_READ))

                .andDo(
                        document("review/read",
                                ApiDocumentUtils.getDocumentRequest(), ApiDocumentUtils.getDocumentResponse(),
                                pathParameters(
                                        parameterWithName("study-product-id").description("스터디 상품 식별자")
                                ),
                                responseFields(
                                        fieldWithPath("status").type(JsonFieldType.NUMBER).description("상태 코드"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),

                                        fieldWithPath("response").type(JsonFieldType.ARRAY).description("조회 데이터"),
                                        fieldWithPath("response[].reviewId").type(JsonFieldType.NUMBER).description("리뷰 식별자, unique"),
                                        fieldWithPath("response[].comment").type(JsonFieldType.STRING).description("한 줄 평"),
                                        fieldWithPath("response[].rating").type(JsonFieldType.NUMBER).description("평점"),
                                        fieldWithPath("response[].lastModifiedTime").type(JsonFieldType.STRING).description("마지막 수정일"),
                                        fieldWithPath("response[].reviewerId").type(JsonFieldType.NUMBER).description("리뷰어 식별자"),
                                        fieldWithPath("response[].reviewerName").type(JsonFieldType.STRING).description("리뷰어 이름"),
                                        fieldWithPath("response[].progress").type(JsonFieldType.STRING).description("스터디 진행률"),
                                        fieldWithPath("response[].likeCnt").type(JsonFieldType.NUMBER).description("리뷰 좋아요 수")
                                )

                        )
                );
    }


    @Test
    void update_review() throws Exception {
        ReviewUpdateForm updateForm = new ReviewUpdateForm(3, "기초를 다루는데 좋은 것 같아요.");

        SimpleMemberDetails memberDetails = new SimpleMemberDetails(1l, "xuni@naver.com", "유니");
        ResultActions result = mockMvc.perform(patch("/reviews/{review-id}", 1l)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(UTF_8)
                .header("Authorization", testJwtTokenProvider.issue(memberDetails))
                .content(objectMapper.writeValueAsString(updateForm))
        );

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(REVIEW_UPDATE))

                .andDo(document("review/update",
                        requestHeaders(
                                headerWithName("Authorization").description("인증 토큰")
                        ),

                        pathParameters(
                                parameterWithName("review-id").description("리뷰 식별자")
                        ),

                        requestFields(
                                fieldWithPath("rating").type(JsonFieldType.NUMBER).description("상품 평점"),
                                fieldWithPath("comment").type(JsonFieldType.STRING).description("상품 한줄평")
                        ),

                        responseFields(
                                fieldWithPath("status").type(JsonFieldType.NUMBER).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                        )
                ));

    }

    @Test
    void delete_review() throws Exception {
        SimpleMemberDetails memberDetails = new SimpleMemberDetails(1l, "xuni@naver.com", "유니");

        ResultActions result = mockMvc.perform(delete("/reviews/{review-id}", 1l)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(UTF_8)
                .header("Authorization", testJwtTokenProvider.issue(memberDetails))
        );

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(REVIEW_DELETE))

                .andDo(document("review/delete",

                        requestHeaders(
                                headerWithName("Authorization").description("인증 토큰")
                        ),

                        pathParameters(
                                parameterWithName("review-id").description("리뷰 식별자")
                        ),

                        responseFields(
                                fieldWithPath("status").type(JsonFieldType.NUMBER).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                        )

                ));
    }
}
package com.xuni.api.statistics.presentation;

import com.xuni.api.auth.application.SimpleMemberDetails;
import com.xuni.api.auth.application.jwt.JwtTokenProvider;
import com.xuni.api.statistics.application.MemberStatisticsService;
import com.xuni.api.statistics.dto.response.ReviewNeedResponse;
import com.xuni.api.support.ControllerCommon;
import com.xuni.api.support.JwtTestConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.test.web.servlet.ResultActions;


import static com.xuni.api.ApiDocumentUtils.*;
import static com.xuni.api.statistics.dto.response.StatisticsApiMessage.REVIEW_NEED_DATA;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Import({StatisticsControllerTestConfig.class, JwtTestConfiguration.class})
class MemberStatisticsControllerTest extends ControllerCommon {

    @Autowired
    JwtTokenProvider testJwtTokenProvider;
    @Autowired
    MemberStatisticsService memberStatisticsService;
    @Test
    void send_progress_docs() throws Exception {
        ReviewNeedResponse response = new ReviewNeedResponse(50);
        BDDMockito.given(memberStatisticsService.readOne(any(), any())).willReturn(response);

        String token = testJwtTokenProvider.issue(new SimpleMemberDetails(1l, "xuni@naver.com", "유니"));
        ResultActions result = mockMvc.perform(get("/statistics/members/{member-id}/study-products/{study-product-id}", 1l, "id")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON));

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(REVIEW_NEED_DATA))

                .andDo(MockMvcRestDocumentation.document("statistics/progress",
                        getDocumentRequest(), getDocumentResponse(),

                        requestHeaders(
                                headerWithName("Authorization").description("인증 토큰")
                        ),

                        pathParameters(
                                parameterWithName("member-id").description("사용자 식별자"),
                                parameterWithName("study-product-id").description("스터디 상품 식별자")
                        ),

                        PayloadDocumentation.responseFields(
                                fieldWithPath("status").type(JsonFieldType.NUMBER).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),

                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("조회 데이터"),
                                fieldWithPath("response.progress").type(JsonFieldType.NUMBER).description("스터디 상품 진행률")
                        )
                ));
    }
}
package com.tw.darkhorse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tw.darkhorse.OrderFixture;
import com.tw.darkhorse.common.ExceptionHandlerAdvice;
import com.tw.darkhorse.common.exception.BusinessException;
import com.tw.darkhorse.common.exception.NoMoreSeatException;
import com.tw.darkhorse.service.impl.OrderServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {OrderController.class})
@ExtendWith(SpringExtension.class)
class OrderControllerTest {

    @Autowired
    private OrderController orderController;

    @MockBean
    private OrderServiceImpl orderServiceImpl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_create_order_successful() throws Exception {
        when(this.orderServiceImpl.createOrder(OrderFixture.getCreateOrderDto())).thenReturn(OrderFixture.getOrder());

        MockMvcBuilders.standaloneSetup(this.orderController).build()
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(OrderFixture.getCreateOrderDto())))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("flight").value("MU2151"))
            .andExpect(jsonPath("classType").value("FIRST"))
            .andExpect(jsonPath("contactMobile").value("13888888888"))
            .andExpect(jsonPath("contactName").value("张三"))
            .andExpect(jsonPath("$.passengerList[0].name").value("李四"))
            .andExpect(jsonPath("$.passengerList[0].ageType").value("老人"))
            .andExpect(jsonPath("$.passengerList[0].mobile").value("13866668888"))
            .andExpect(jsonPath("$.passengerList[0].insuranceId").value("666"))
            .andExpect(jsonPath("$.passengerList[0].insuranceName").value("一路顺风"))
            .andExpect(jsonPath("$.passengerList[0].insurancePrice").value(20))
            .andExpect(jsonPath("$.passengerList[0].identificationNumber").value("610502200001015432"))
            .andExpect(jsonPath("$.passengerList[0].price").value(200))
            .andExpect(jsonPath("$.orderEventList[0].status").value("CREATED"))
            .andExpect(jsonPath("userId").value(12L));
    }

    @Test
    void should_create_order_failed_when_no_more_seat() throws Exception {
        doThrow(NoMoreSeatException.class).when(this.orderServiceImpl).createOrder(OrderFixture.getCreateOrderDto());

        MockMvcBuilders.standaloneSetup(this.orderController).setControllerAdvice(ExceptionHandlerAdvice.class).build()
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(OrderFixture.getCreateOrderDto())))
            .andExpect(status().isNotFound())
            .andExpect(content().string("机票已售罄"));
    }

    @Test
    void should_create_order_failed_when_identity_id_is_valid() throws Exception {
        MockMvcBuilders.standaloneSetup(this.orderController).setControllerAdvice(ExceptionHandlerAdvice.class).build()
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(OrderFixture.getCreateOrderDtoWithInvalidIdentityNumber())))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("code").value("10001"))
            .andExpect(jsonPath("message").value("身份证号码格式有误"));
    }

    @Test
    void should_throw_exception_when_server_has_problem() throws Exception {
        given(orderServiceImpl.createOrder(any())).willThrow(BusinessException.class);

        MockMvcBuilders.standaloneSetup(this.orderController).setControllerAdvice(ExceptionHandlerAdvice.class).build()
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(OrderFixture.getCreateOrderDto())))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("服务异常，请稍后再试"));
    }

    @Test
    void should_throw_exception_when_call_dependent_server_failed() throws Exception {
        given(orderServiceImpl.createOrder(any())).willThrow(FeignException.FeignServerException.class);

        MockMvcBuilders.standaloneSetup(this.orderController).setControllerAdvice(ExceptionHandlerAdvice.class).build()
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(OrderFixture.getCreateOrderDto())))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("服务异常，请稍后再试"));
    }

    @Test
    void should_cancel_order_successful() throws Exception {
        doNothing().when(this.orderServiceImpl).cancelOrder(anyLong());
        MockMvcBuilders.standaloneSetup(this.orderController).build()
            .perform(post("/orders/{orderId}/cancellation", 123L))
            .andDo(print())
            .andExpect(status().isOk());
    }


    @Test
    void should_cancel_order_failed() throws Exception {
        doThrow(BusinessException.class).when(this.orderServiceImpl).cancelOrder(any());
        MockMvcBuilders.standaloneSetup(this.orderController).setControllerAdvice(ExceptionHandlerAdvice.class)
                .build()
                .perform(post("/orders/{orderId}/cancellation", 123L))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("服务异常，请稍后再试"));
    }

    @Test
    void should_throw_exception_when_call_third_party_failed() throws Exception {
        doThrow(FeignException.FeignServerException.class).when(this.orderServiceImpl).cancelOrder(any());
        MockMvcBuilders.standaloneSetup(this.orderController).setControllerAdvice(ExceptionHandlerAdvice.class)
            .build()
            .perform(post("/orders/{orderId}/cancellation", 123L))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("服务异常，请稍后再试"));
    }

}


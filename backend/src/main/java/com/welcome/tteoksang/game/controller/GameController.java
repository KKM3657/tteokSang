package com.welcome.tteoksang.game.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.welcome.tteoksang.game.dto.Chat;
import com.welcome.tteoksang.game.dto.GameMessage;
import com.welcome.tteoksang.game.dto.res.GameMessageRes;
import com.welcome.tteoksang.game.dto.res.WebSocketIdRes;
import com.welcome.tteoksang.game.dto.result.TestExample;
import com.welcome.tteoksang.game.dto.result.end.FinalResult;
import com.welcome.tteoksang.game.dto.result.half.Half;
import com.welcome.tteoksang.game.dto.result.offline.OfflineReport;
import com.welcome.tteoksang.game.dto.server.ServerInfo;
import com.welcome.tteoksang.game.service.ChatService;
import com.welcome.tteoksang.game.service.ReportService;
import com.welcome.tteoksang.redis.RedisPrefix;
import com.welcome.tteoksang.redis.RedisService;
import com.welcome.tteoksang.resource.type.MessageType;
import com.welcome.tteoksang.user.dto.User;
import com.welcome.tteoksang.user.service.UserService;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

@Controller
@Slf4j
@RequiredArgsConstructor
public class GameController {

    private final RedisService redisService;
    private final ChatService chatService;
    private final ReportService reportService;

    private final SimpMessagingTemplate simpMessagingTemplate;

    @GetMapping("/web-socket")
    public ResponseEntity<WebSocketIdRes> createWebSocketId(@AuthenticationPrincipal User user) {
        String webSocketUUID = UUID.randomUUID().toString();

        String key = RedisPrefix.WEBSOCKET.prefix() + user.getUserId();
        // 레디스에 webSocketId 저장
        redisService.setValues(key, webSocketUUID);

        return ResponseEntity.ok().
                body(
                        WebSocketIdRes.builder()
                                .webSocketId(webSocketUUID)
                                .build()
                );
    }

    @GetMapping("/quarter/{webSocketId}")
    public void sendQuarter(@PathVariable("webSocketId") String webSocketId, @AuthenticationPrincipal User user) {
//        String logData = TestExample.quarter;
//        ObjectMapper mapper = new ObjectMapper();
//        boolean isSuccess = false;
//        Quarter message = null;
//        try {
//            message = mapper.readValue(logData, Quarter.class);
//            isSuccess = true;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        GameMessageRes quarterResult = GameMessageRes.builder()
//                .type(MessageType.QUARTER_REPORT)
//                .isSuccess(isSuccess)
//                .body(message)
//                .build();
//
//        simpMessagingTemplate.convertAndSend("/topic/private/" + webSocketId, quarterResult);

        GameMessageRes quarterResult = reportService.sendQuarterResult(user.getUserId());
        simpMessagingTemplate.convertAndSend("/topic/private/" + webSocketId, quarterResult);
    }

    @GetMapping("/half/{webSocketId}")
    public void sendHalf(@PathVariable("webSocketId") String webSocketId) {
        String responseData = TestExample.half;
        ObjectMapper mapper = new ObjectMapper();
        boolean isSuccess = false;
        Half message = null;
        try {
            message = mapper.readValue(responseData, Half.class);
            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        GameMessageRes halfResult = GameMessageRes.builder()
                .type(MessageType.HALF_REPORT)
                .isSuccess(isSuccess)
                .body(message)
                .build();

        simpMessagingTemplate.convertAndSend("/topic/private/" + webSocketId, halfResult);
    }

    @GetMapping("/final/{webSocketId}")
    public void sendFinal(@PathVariable("webSocketId") String webSocketId) {
        String responseData = TestExample.finalReport;
        ObjectMapper mapper = new ObjectMapper();
        boolean isSuccess = false;
        FinalResult message = null;
        try {
            message = mapper.readValue(responseData, FinalResult.class);
            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        GameMessageRes finalResult = GameMessageRes.builder()
                .type(MessageType.FINAL_REPORT)
                .isSuccess(isSuccess)
                .body(message)
                .build();

        simpMessagingTemplate.convertAndSend("/topic/private/" + webSocketId, finalResult);
    }

    @GetMapping("/offline/{webSocketId}")
    public void sendOffline(@PathVariable("webSocketId") String webSocketId) {
        String ResponseData = TestExample.offlineData;
        ObjectMapper mapper = new ObjectMapper();
        boolean isSuccess = false;
        OfflineReport message = null;
        try {
            message = mapper.readValue(ResponseData, OfflineReport.class);
            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        GameMessageRes offlineResult = GameMessageRes.builder()
                .type(MessageType.OFFLINE_REPORT)
                .isSuccess(isSuccess)
                .body(message)
                .build();

        simpMessagingTemplate.convertAndSend("/topic/private/" + webSocketId, offlineResult);
    }

    @GetMapping("/hello")
    public ResponseEntity<?> sendKafka() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        // 보낼 키
        String key = "c4895cb2-5e73-4bb8-9c90-1026983ef509::1";
        // 보낼 값
        String value = "{\"totalAccPrivateBrokerFee\":0,\"accPrivateRentFee\":0,\"totalAccPrivateProductIncome\":0,\"totalAccPrivateProductOutcome\":778,\"accPrivateUpgradeFee\":0,\"accPrivateEventBonus\":0,\"totalAccPrivateProductProfit\":0,\"maxPrivateProductOutcome\":778,\"maxPrivateProductIncome\":0,\"maxPrivateProductProfit\":0,\"accPrivatePlayTime\":0,\"accPrivateOnlineTimeSlotCount\":[0,0,0,0,0,0,0,0],\"accPrivateGamePlayCount\":1,\"accPrivateEventOccurId\":0,\"reduceProductInfoMap\":{\"0\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"1\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"2\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"3\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"4\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"5\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"6\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"7\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"8\":{\"accPrivateProductPurchaseQuantity\":1,\"maxPrivateProductPurchaseQuantity\":1,\"accPrivateProductOutcome\":778,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":1,\"maxPrivateProductPurchaseCost\":778,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"9\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"10\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"11\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"12\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"13\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"14\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"15\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"16\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"17\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"18\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"19\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"20\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"21\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"22\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"23\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"24\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"25\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"26\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"27\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"28\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"29\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"30\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"31\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"32\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"33\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"34\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"35\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"36\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"37\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"38\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"39\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"40\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"41\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"42\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"43\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"44\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"45\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"46\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"47\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"48\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"49\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"50\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"51\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"52\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"53\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"54\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"55\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0},\"56\":{\"accPrivateProductPurchaseQuantity\":0,\"maxPrivateProductPurchaseQuantity\":0,\"accPrivateProductOutcome\":0,\"accPrivateProductSalesQuantity\":0,\"maxPrivateProductSalesQuantity\":0,\"accPrivateProductIncome\":0,\"accPrivateProductProfit\":0,\"maxPrivateProductProfit\":0,\"maxPrivateProductHoldingQuantity\":0,\"maxPrivateProductPurchaseCost\":0,\"minPrivateProductPurchaseCost\":0,\"maxPrivateProductSalesCost\":0,\"minPrivateProductSalesCost\":0,\"accPrivateBrokerFee\":0}}}";
        // 메시지를 'tteoksang_hadoop' 토픽에 보냄
        producer.send(new ProducerRecord<>("tteoksang_hadoop", key, value));
        System.out.println("메시지 전송 완료" );

        producer.send((new ProducerRecord<>("tteoksang_hadoop", key, "END")));
        return ResponseEntity.ok().body("good");
    }


    @MessageMapping("/chat") // 클라이언트에서 보낸 메시지를 받을 메서드 지정
    @SendTo("/topic/chat") // 메서드가 처리한 결과를 보낼 목적지 지정
    public GameMessage handleChatMessage(@Payload GameMessage gameMessage,
                                         SimpMessageHeaderAccessor headerAccessor) {
          /* @DestinationVariable: 메시지의 목적지에서 변수를 추출
             @Payload: 메시지 본문(body)의 내용을 메서드의 인자로 전달할 때 사용
                      (클라이언트가 JSON 형태의 메시지를 보냈다면, 이를 GameMessage 객체로 변환하여 메서드에 전달)
          */
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        Map<String, Object> body = (Map<String, Object>) gameMessage.getBody();
        String message = (String) body.get("message");
        log.debug("메세지 내용 :{}", message);
        Chat chat = chatService.sendChat(userId, (Map<String, Object>) gameMessage.getBody());
        if (chat == null) return null;
        gameMessage.setBody(chat);
        return gameMessage;
    }
}

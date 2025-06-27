package com.wallet.money.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.money.carteclient.CardRechargeRequest;
import com.wallet.money.service.DepotMoneyService;
import com.wallet.money.service.TransactionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardRechargeController.class)
public class CardRechargeControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private DepotMoneyService depotMoneyService;

	@MockBean
	private TransactionService transactionService;

	@Test
	public void initiateCardRecharge() throws Exception {
		CardRechargeRequest request = new CardRechargeRequest();
		String clientId = "abc";
		String sourceService = "abc";
		ObjectMapper objectMapper = new ObjectMapper();
		String json = objectMapper.writeValueAsString(sourceService);
		this.mockMvc.perform(post("/api/money/card-recharge").content(json).contentType(MediaType.APPLICATION_JSON_VALUE))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
			.andExpect(jsonPath("$.requestId").value("<value>"))
			.andExpect(jsonPath("$.idCarte").value("<value>"))
			.andExpect(jsonPath("$.montant").value("<value>"))
			.andExpect(jsonPath("$.status").value("<value>"))
			.andExpect(jsonPath("$.message").value("<value>"))
			.andExpect(jsonPath("$.freemoReference").value("<value>"))
			.andExpect(jsonPath("$.timestamp").value("<value>"));
	}
}

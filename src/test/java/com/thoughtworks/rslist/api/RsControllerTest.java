package com.thoughtworks.rslist.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RsControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired UserRepository userRepository;
  @Autowired RsEventRepository rsEventRepository;
  @Autowired VoteRepository voteRepository;
  @Autowired TradeRepository tradeRepository;
  private UserDto userDto;
  ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    voteRepository.deleteAll();
    rsEventRepository.deleteAll();
    userRepository.deleteAll();
    userDto =
        UserDto.builder()
            .voteNum(10)
            .phone("188888888888")
            .gender("female")
            .email("a@b.com")
            .age(19)
            .userName("idolice")
            .build();
  }

  @Test
  public void shouldGetRsEventList() throws Exception {
    UserDto save = userRepository.save(userDto);

    RsEventDto rsEventDto =
        RsEventDto.builder().keyword("no").eventName("1").user(save).build();

    rsEventRepository.save(rsEventDto);

    mockMvc
        .perform(get("/rs/list"))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].eventName", is("1")))
        .andExpect(jsonPath("$[0].keyword", is("no")))
        .andExpect(jsonPath("$[0]", not(hasKey("user"))))
        .andExpect(status().isOk());
  }

  @Test
  public void shouldGetOneEvent() throws Exception {
    UserDto save = userRepository.save(userDto);

    RsEventDto rsEventDto =
        RsEventDto.builder().keyword("no").eventName("1").user(save).build();

    rsEventRepository.save(rsEventDto);
    rsEventDto = RsEventDto.builder().keyword("no").eventName("2").user(save).build();
    rsEventRepository.save(rsEventDto);
    mockMvc.perform(get("/rs/1")).andExpect(jsonPath("$.eventName", is("1")));
    mockMvc.perform(get("/rs/1")).andExpect(jsonPath("$.keyword", is("no")));
    mockMvc.perform(get("/rs/2")).andExpect(jsonPath("$.eventName", is("2")));
    mockMvc.perform(get("/rs/2")).andExpect(jsonPath("$.keyword", is("no")));
  }

  @Test
  public void shouldGetErrorWhenIndexInvalid() throws Exception {
    mockMvc
        .perform(get("/rs/4"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", is("invalid index")));
  }

  @Test
  public void shouldGetRsListBetween() throws Exception {
    UserDto save = userRepository.save(userDto);

    RsEventDto rsEventDto =
        RsEventDto.builder().keyword("no").eventName("1").user(save).build();

    rsEventRepository.save(rsEventDto);
    rsEventDto = RsEventDto.builder().keyword("no").eventName("2").user(save).build();
    rsEventRepository.save(rsEventDto);
    rsEventDto = RsEventDto.builder().keyword("no").eventName("3").user(save).build();
    rsEventRepository.save(rsEventDto);
    mockMvc
        .perform(get("/rs/list?start=1&end=2"))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].eventName", is("1")))
        .andExpect(jsonPath("$[0].keyword", is("no")))
        .andExpect(jsonPath("$[1].eventName", is("2")))
        .andExpect(jsonPath("$[1].keyword", is("no")));
    mockMvc
        .perform(get("/rs/list?start=2&end=3"))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].eventName", is("2")))
        .andExpect(jsonPath("$[0].keyword", is("no")))
        .andExpect(jsonPath("$[1].eventName", is("3")))
        .andExpect(jsonPath("$[1].keyword", is("no")));
    mockMvc
        .perform(get("/rs/list?start=1&end=3"))
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].keyword", is("no")))
        .andExpect(jsonPath("$[1].eventName", is("2")))
        .andExpect(jsonPath("$[1].keyword", is("no")))
        .andExpect(jsonPath("$[2].eventName", is("3")))
        .andExpect(jsonPath("$[2].keyword", is("no")));
  }

  @Test
  public void shouldAddRsEventWhenUserExist() throws Exception {

    UserDto save = userRepository.save(userDto);

    String jsonValue =
        "{\"eventName\":\"a\",\"keyword\":\"k\",\"userId\": " + save.getId() + "}";

    mockMvc
        .perform(post("/rs/event").content(jsonValue).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());
    List<RsEventDto> all = rsEventRepository.findAll();
    assertNotNull(all);
    assertEquals(all.size(), 1);
    assertEquals(all.get(0).getEventName(), "a");
    assertEquals(all.get(0).getKeyword(), "k");
    assertEquals(all.get(0).getUser().getUserName(), save.getUserName());
    assertEquals(all.get(0).getUser().getAge(), save.getAge());
  }

  @Test
  public void shouldAddRsEventWhenUserNotExist() throws Exception {
    String jsonValue = "{\"eventName\":\"a\",\"keyword\":\"k\",\"userId\": 100}";
    mockMvc
        .perform(post("/rs/event").content(jsonValue).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void shouldVoteSuccess() throws Exception {
    UserDto save = userRepository.save(userDto);
    RsEventDto rsEventDto =
        RsEventDto.builder().keyword("no").eventName("1").user(save).build();
    rsEventDto = rsEventRepository.save(rsEventDto);

    String jsonValue =
        String.format(
            "{\"userId\":%d,\"time\":\"%s\",\"voteNum\":1}",
            save.getId(), LocalDateTime.now().toString());
    mockMvc
        .perform(
            post("/rs/vote/{id}", rsEventDto.getId())
                .content(jsonValue)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    UserDto userDto = userRepository.findById(save.getId()).get();
    RsEventDto newRsEvent = rsEventRepository.findById(rsEventDto.getId()).get();
    assertEquals(userDto.getVoteNum(), 9);
    assertEquals(newRsEvent.getVoteNum(), 1);
    List<VoteDto> voteDtos =  voteRepository.findAll();
    assertEquals(voteDtos.size(), 1);
    assertEquals(voteDtos.get(0).getNum(), 1);
  }
  @Test
  void shouldGetOrderedRsEvent() throws Exception {
    UserDto save = userRepository.save(userDto);

    RsEventDto rsEventDto1 = RsEventDto.builder().keyword("no").eventName("1").voteNum(32).user(save).build();
    RsEventDto rsEventDto2 = RsEventDto.builder().keyword("no").eventName("2").voteNum(16).user(save).build();
    RsEventDto rsEventDto3 = RsEventDto.builder().keyword("no").eventName("3").voteNum(8).user(save).build();
    RsEventDto rsEventDto4 = RsEventDto.builder().keyword("no").eventName("4").voteNum(4).tradeRank(4).user(save).build();
    RsEventDto rsEventDto5 = RsEventDto.builder().keyword("no").eventName("5").voteNum(2).tradeRank(1).user(save).build();
    RsEventDto rsEventDto6 = RsEventDto.builder().keyword("no").eventName("6").voteNum(1).tradeRank(2).user(save).build();
    rsEventRepository.save(rsEventDto1);
    rsEventRepository.save(rsEventDto2);
    rsEventRepository.save(rsEventDto3);
    rsEventRepository.save(rsEventDto4);
    rsEventRepository.save(rsEventDto5);
    rsEventRepository.save(rsEventDto6);

    mockMvc.perform(get("/rs/list"))
            .andExpect(jsonPath("$[0].eventName", is("5")))
            .andExpect(jsonPath("$[1].eventName", is("6")))
            .andExpect(jsonPath("$[2].eventName", is("1")))
            .andExpect(jsonPath("$[3].eventName", is("4")))
            .andExpect(jsonPath("$[4].eventName", is("2")))
            .andExpect(jsonPath("$[5].eventName", is("3")))
            .andExpect(status().isOk());
  }

  @Test
  void shouldAddTradeWhenRankIsNotTraded() throws Exception {
    UserDto save = userRepository.save(userDto);
    RsEventDto rsEventDto = RsEventDto.builder().keyword("no").eventName("1").voteNum(1).user(save).build();
    rsEventRepository.save(rsEventDto);
    Trade trade = Trade.builder().amount(1).rank(1).build();
    String jsonRequest = objectMapper.writeValueAsString(trade);
    int idToBuy = rsEventRepository.findAll().get(0).getId();

    mockMvc.perform(post("/rs/buy/" + idToBuy).content(jsonRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    assertEquals(1, tradeRepository.findAll().size());
  }
  @Test
  void shouldAddTradeWhenRankIsTradedAndAmountIsEnough() throws Exception {
    UserDto save = userRepository.save(userDto);
    RsEventDto rsEventDto = RsEventDto.builder().keyword("no").eventName("1").voteNum(1).user(save).build();
    rsEventRepository.save(rsEventDto);
    TradeDto tradeDtoExists = TradeDto.builder().amount(10).rank(1).build();
    tradeRepository.save(tradeDtoExists);
    int idToBuy = rsEventRepository.findAll().get(0).getId();

    Trade trade = Trade.builder().amount(15).rank(1).build();
    String jsonRequest = objectMapper.writeValueAsString(trade);

    mockMvc.perform(post("/rs/buy/"+idToBuy).content(jsonRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

    assertEquals(1, tradeRepository.findAll().size());
  }

  @Test
  void shouldUpdateRankOfRsEventAndDeleteOldRsEventWhenAmountIsEnough() throws Exception {
    UserDto save = userRepository.save(userDto);
    RsEventDto rsEventDtoExists = RsEventDto.builder().eventName("alread have").keyword("1").tradeRank(1).user(save).build();
    RsEventDto rsEventDtoToTrade = RsEventDto.builder().eventName("new buy").keyword("2").user(save).build();
    TradeDto tradeDtoExists = TradeDto.builder().amount(10).rank(1).build();
    rsEventRepository.save(rsEventDtoToTrade);
    int idToTrade = rsEventRepository.findAll().get(0).getId();
    rsEventRepository.save(rsEventDtoExists);
    tradeRepository.save(tradeDtoExists);

    Trade trade = Trade.builder().amount(15).rank(1).build();
    String jsonRequest = objectMapper.writeValueAsString(trade);

    mockMvc.perform(post("/rs/buy/"+idToTrade).content(jsonRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

    mockMvc.perform(get("/rs/list"))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].eventName", is("new buy")))
            .andExpect(jsonPath("$[0].tradeRank", is(1)))
            .andExpect(status().isOk());
    assertEquals(1, tradeRepository.findAll().size());
  }


}

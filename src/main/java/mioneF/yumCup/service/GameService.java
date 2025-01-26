package mioneF.yumCup.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mioneF.yumCup.domain.MatchResult;
import mioneF.yumCup.domain.dto.response.MatchResponse;
import mioneF.yumCup.domain.dto.response.RestaurantResponse;
import mioneF.yumCup.domain.entity.Game;
import mioneF.yumCup.domain.entity.Match;
import mioneF.yumCup.domain.entity.Restaurant;
import mioneF.yumCup.repository.GameRepository;
import mioneF.yumCup.repository.MatchRepository;
import mioneF.yumCup.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게임 진행 로직을 추상화하여 다양한 토너먼트 방식을 지원 예정
 */
@Service
@RequiredArgsConstructor
public class GameService {
    private final MatchRepository matchRepository;
    private final GameRepository gameRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public MatchResult selectWinner(Long gameId, Long matchId, Long winnerId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        Match currentMatch = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        Restaurant winner = restaurantRepository.findById(winnerId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

        // 현재 매치 승자 설정
        currentMatch.setWinner(winner);

        // 다음 라운드 진출자 목록 관리 및 다음 매치 반환
        return processNextMatch(game, currentMatch);
    }

    private MatchResult processNextMatch(Game game, Match currentMatch) {
        List<Match> matches = game.getMatches();
        int currentRound = currentMatch.getRound();
        int currentOrder = currentMatch.getMatchOrder();

        // 현재 라운드의 모든 매치가 완료되었는지 확인
        boolean isRoundComplete = isCurrentRoundComplete(matches, currentRound);

        if (isRoundComplete) {
            if (currentRound == 2) {
                return handleFinalRound(game, currentMatch);
            }

            return createNextRoundMatches(game, matches, currentRound);
        }

        // 현재 라운드의 다음 매치 반환
        return createCurrentRoundNextMatch(matches, currentRound, currentOrder);
    }

    // 현재 라운드의 모든 매치가 완료되었는지 확인하는 메서드
    private boolean isCurrentRoundComplete(List<Match> matches, int currentRound) {
        return matches.stream()
                .filter(m -> m.getRound() == currentRound)
                .allMatch(m -> m.getWinner() != null);
    }

    // 결승전 처리 메서드
    private MatchResult handleFinalRound(Game game, Match currentMatch) {
        Restaurant finalWinner = currentMatch.getWinner();
        game.complete(finalWinner);
        finalWinner.incrementWinCount();

        restaurantRepository.save(finalWinner);
        gameRepository.save(game);

        return new MatchResult(
                true, null, RestaurantResponse.from(finalWinner));
    }

    // 다음 라운드의 매치들을 생성하는 메서드
    private MatchResult createNextRoundMatches(Game game, List<Match> matches, int currentRound) {
        List<Restaurant> winners = getWinnersFromCurrentRound(matches, currentRound);

        int nextRound = currentRound / 2;
        List<Match> nextMatches = createMatchesForNextRound(winners, nextRound);

        game.addMatch(nextMatches.get(0));  // 첫 번째 매치 추가
        for (int i = 1; i < nextMatches.size(); i++) {
            game.addMatch(nextMatches.get(i));  // 나머지 매치들 추가
        }

        matchRepository.saveAll(nextMatches);

        Match nextMatch = nextMatches.get(0);
        return new MatchResult(
                false,
                new MatchResponse(
                        nextMatch.getId(),
                        RestaurantResponse.from(nextMatch.getRestaurant1()),
                        RestaurantResponse.from(nextMatch.getRestaurant2()),
                        nextMatch.getRound(),
                        nextMatch.getMatchOrder()
                ),
                null
        );
    }

    // 현재 라운드의 승자들을 수집하는 메서드
    private List<Restaurant> getWinnersFromCurrentRound(List<Match> matches, int currentRound) {
        return matches.stream()
                .filter(m -> m.getRound() == currentRound)
                .map(Match::getWinner)
                .collect(Collectors.toList());
    }

    // 다음 라운드의 매치들을 생성하는 메서드
    private List<Match> createMatchesForNextRound(List<Restaurant> winners, int nextRound) {
        List<Match> nextMatches = new ArrayList<>();
        for (int i = 0; i < winners.size(); i += 2) {
            Match match = Match.builder()
                    .restaurant1(winners.get(i))
                    .restaurant2(winners.get(i + 1))
                    .round(nextRound)
                    .matchOrder(i / 2 + 1)
                    .build();
            nextMatches.add(match);
        }
        return nextMatches;
    }

    // 현재 라운드의 다음 매치 결과를 생성하는 메서드
    private MatchResult createCurrentRoundNextMatch(List<Match> matches, int currentRound, int currentOrder) {
        Match nextMatch = matches.stream()
                .filter(m -> m.getRound() == currentRound && m.getMatchOrder() == currentOrder + 1)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Next match not found"));

        return new MatchResult(
                false,
                new MatchResponse(
                        nextMatch.getId(),
                        RestaurantResponse.from(nextMatch.getRestaurant1()),
                        RestaurantResponse.from(nextMatch.getRestaurant2()),
                        nextMatch.getRound(),
                        nextMatch.getMatchOrder()
                ),
                null
        );
    }
}

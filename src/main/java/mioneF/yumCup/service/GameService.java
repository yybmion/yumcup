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
        boolean isRoundComplete = matches.stream()
                .filter(m -> m.getRound() == currentRound)
                .allMatch(m -> m.getWinner() != null);

        if (isRoundComplete) {
            if (currentRound == 2) {
                // 결승전 종료
                Restaurant finalWinner = currentMatch.getWinner();
                game.complete(finalWinner);
                finalWinner.incrementWinCount();

                // 최종 결과 저장
                restaurantRepository.save(finalWinner);
                gameRepository.save(game);

                return new MatchResult(
                        true, null, RestaurantResponse.from(finalWinner));
            }

            // 다음 라운드 매치들 생성
            List<Restaurant> winners = matches.stream()
                    .filter(m -> m.getRound() == currentRound)
                    .map(Match::getWinner)
                    .collect(Collectors.toList());

            int nextRound = currentRound / 2;
            List<Match> nextMatches = new ArrayList<>();

            for (int i = 0; i < winners.size(); i += 2) {
                Match match = Match.builder()
                        .restaurant1(winners.get(i))
                        .restaurant2(winners.get(i + 1))
                        .round(nextRound)
                        .matchOrder(i / 2 + 1)
                        .build();
                game.addMatch(match);
                nextMatches.add(match);
            }

            matchRepository.saveAll(nextMatches);

            // 5. 다음 라운드의 첫 매치 정보 반환
            Match nextMatch = nextMatches.get(0);
            return new MatchResult(
                    false,  // gameComplete
                    new MatchResponse(
                            nextMatch.getId(),
                            RestaurantResponse.from(nextMatch.getRestaurant1()),
                            RestaurantResponse.from(nextMatch.getRestaurant2()),
                            nextMatch.getRound(),
                            nextMatch.getMatchOrder()
                    ),
                    null  // winner (아직 게임 진행 중)
            );
        }

        // 6. 현재 라운드의 다음 매치 반환
        Match nextMatch = matches.stream()
                .filter(m -> m.getRound() == currentRound && m.getMatchOrder() == currentOrder + 1)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Next match not found"));

        return new MatchResult(
                false,  // gameComplete
                new MatchResponse(
                        nextMatch.getId(),
                        RestaurantResponse.from(nextMatch.getRestaurant1()),
                        RestaurantResponse.from(nextMatch.getRestaurant2()),
                        nextMatch.getRound(),
                        nextMatch.getMatchOrder()
                ),
                null  // winner (아직 게임 진행 중)
        );
    }
}

package com.a101.ecofarming.challenge.batch.tasklet;

import com.a101.ecofarming.balanceGame.entity.BalanceGame;
import com.a101.ecofarming.balanceGame.repository.BalanceGameRepository;
import com.a101.ecofarming.challenge.entity.Challenge;
import com.a101.ecofarming.challenge.repository.ChallengeRepository;
import com.a101.ecofarming.global.exception.CustomException;
import com.a101.ecofarming.global.notification.NotificationManager;
import com.a101.ecofarming.global.notification.fcm.FCMService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import static com.a101.ecofarming.global.exception.ErrorCode.BALANCE_GAME_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class ChallengeRegenerationTasklet implements Tasklet {

    private final NotificationManager notificationManager;
    private final FCMService fcmService;
    private final ChallengeRepository challengeRepository;
    private final BalanceGameRepository balanceGameRepository;

    @Override
    public RepeatStatus execute(@NonNull StepContribution contribution, ChunkContext chunkContext) {
        LocalDate today = (LocalDate) chunkContext.getStepContext().getJobParameters().get("today");
        List<Challenge> endingChallenges = challengeRepository.findChallengesEndingByDate(today);
        
        // 밸런스 게임 넘버링(밸런스 게임 순회)
        int lastBalanceId = challengeRepository.findMaxBalanceId(today);
        int totalBalanceGameCount = balanceGameRepository.getTotalBalanceGameCount();
        int nextBalanceId = (lastBalanceId % totalBalanceGameCount) + 1;
        
        // 새로운 챌린지 생성
        for (Challenge challenge : endingChallenges) {

            // 기존 알림 구독을 취소하고 알림을 보낸다.
            notificationManager.sendNotification(challenge, 1);
            //fcmService.unsubscribeFromTopic("");

            BalanceGame newBalanceGame = balanceGameRepository.findById(nextBalanceId)
                    .orElseThrow(() -> new CustomException(BALANCE_GAME_NOT_FOUND));
            Challenge newChallenge = Challenge.builder()
                    .startDate(today.plusDays(1))
                    .endDate(today.plusDays(challenge.getDuration()))
                    .frequency(challenge.getFrequency())
                    .duration(challenge.getDuration())
                    .totalBetAmountOption1(0)
                    .totalBetAmountOption2(0)
                    .userCount(0)
                    .balanceGame(newBalanceGame)
                    .challengeCategory(challenge.getChallengeCategory())
                    .build();
            challengeRepository.save(newChallenge);
            // 밸런스 게임이 여러 개일 때를 대비해 숫자를 하나씩 올림
            nextBalanceId = (nextBalanceId % totalBalanceGameCount) + 1;

            // 새로운 챌린지가 시작되면 알림을 전송한다.
            notificationManager.sendNotification(newChallenge, 2);
        }
        return RepeatStatus.FINISHED;
    }
}

package tech.alaz.git.project.score.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.alaz.git.project.score.domain.score.ScoreOverHundred;

public class ScoreDto {
    private ScoreOverHundred totalScore;
    private ScoreOverHundred recencyScore;
    private ScoreOverHundred starGazersScore;
    private ScoreOverHundred forksScore;

    public ScoreDto(ScoreOverHundred totalScore, ScoreOverHundred recencyScore, ScoreOverHundred starGazersScore, ScoreOverHundred forksScore) {
        this.totalScore = totalScore;
        this.recencyScore = recencyScore;
        this.starGazersScore = starGazersScore;
        this.forksScore = forksScore;
    }

    @JsonProperty("totalScore")
    public int getTotalScore() {
        return totalScore.getScore();
    }

    @JsonProperty("recencyScore")
    public int getRecencyScore() {
        return recencyScore.getScore();
    }

    @JsonProperty("starGazersScore")
    public int getStarGazersScore() {
        return starGazersScore.getScore();
    }

    @JsonProperty("forksScore")
    public int getForksScore() {
        return forksScore.getScore();
    }
}

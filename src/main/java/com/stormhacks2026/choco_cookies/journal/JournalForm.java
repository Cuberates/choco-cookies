package com.stormhacks2026.choco_cookies.journal;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

public class JournalForm {
    @NotNull @PastOrPresent @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate bowlingDate = LocalDate.now();
    @NotBlank @Size(max = 160) private String leagueName = "";
    @NotBlank @Size(max = 160) private String alleyName = "";
    @NotBlank @Size(max = 160) private String location = "";
    @NotNull @Min(1) @Max(100) private Integer games;
    @NotNull @Min(0) private Integer pins;
    @Size(max = 2000) private String notes = "";
    @Size(max = 6, message = "Choose at most six balls.")
    private List<@NotNull Long> ballIds = new ArrayList<>();

    public static JournalForm from(JournalEntry entry) {
        var form = new JournalForm();
        form.bowlingDate = entry.getBowlingDate(); form.leagueName = entry.getLeagueName();
        form.alleyName = entry.getAlleyName(); form.location = entry.getLocation();
        form.games = entry.getGames(); form.pins = entry.getPins(); form.notes = entry.getNotes();
        form.ballIds = entry.getBalls().stream().map(ball -> ball.getId()).toList();
        return form;
    }
    @AssertTrue(message = "Total pin-fall cannot exceed 300 per game.")
    public boolean isScoreValid() { return games == null || pins == null || pins <= (long) games * 300; }
    @AssertTrue(message = "Bowling date must be on or after January 1, 1900.")
    public boolean isDateInRange() { return bowlingDate == null || !bowlingDate.isBefore(LocalDate.of(1900, 1, 1)); }
    public LocalDate getBowlingDate() { return bowlingDate; }
    public void setBowlingDate(LocalDate value) { bowlingDate = value; }
    public String getLeagueName() { return leagueName; }
    public void setLeagueName(String value) { leagueName = trim(value); }
    public String getAlleyName() { return alleyName; }
    public void setAlleyName(String value) { alleyName = trim(value); }
    public String getLocation() { return location; }
    public void setLocation(String value) { location = trim(value); }
    public Integer getGames() { return games; }
    public void setGames(Integer value) { games = value; }
    public Integer getPins() { return pins; }
    public void setPins(Integer value) { pins = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = trim(value); }
    public List<Long> getBallIds() { return ballIds; }
    public void setBallIds(List<Long> value) { ballIds = value == null ? new ArrayList<>() : value; }
    private static String trim(String value) { return value == null ? "" : value.trim(); }
}

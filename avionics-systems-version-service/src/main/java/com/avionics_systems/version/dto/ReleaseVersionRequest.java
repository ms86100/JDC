package com.avionics_systems.version.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseVersionRequest {
    private UUID releasedBy;
    private LocalDateTime actualReleaseDate;
    private String releaseNotesUrl;
    private Boolean generateReleaseNotes;
}
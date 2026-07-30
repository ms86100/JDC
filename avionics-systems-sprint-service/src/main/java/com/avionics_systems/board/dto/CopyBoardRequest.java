package com.avionics_systems.board.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyBoardRequest {
    private String name;
}

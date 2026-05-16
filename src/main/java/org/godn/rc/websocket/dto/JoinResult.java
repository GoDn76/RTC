package org.godn.rc.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class JoinResult {
    private List<Chat> history;
    private Chat systemMessage;

    public JoinResult(List<Chat> history) {
        this.history = history;
    }
}

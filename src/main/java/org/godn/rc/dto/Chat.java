package org.godn.rc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Chat {
    private String id;
    private String userId;
    private String roomId;
    private String name;
    private String message;
    private Set<String> upvotes = new HashSet<>();
    private Long timestamp = System.currentTimeMillis();
}

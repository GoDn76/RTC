package org.godn.rc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveRoom {
    private String roomId;
    private Set<User> users = ConcurrentHashMap.newKeySet();
}

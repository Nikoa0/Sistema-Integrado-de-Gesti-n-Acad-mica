package com.ipt.siga.entities;

import com.ipt.siga.enums.ClassDay;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ScheduleBlock {

    @Enumerated(EnumType.STRING)
    @Column(name = "day_code", nullable = false, length = 1)
    private ClassDay day;

    @Column(name = "module_number", nullable = false)
    private Integer moduleNumber;

    // "L2" -> day L, module 2
    public static ScheduleBlock fromCode(String code) {
        return new ScheduleBlock(
                ClassDay.valueOf(code.substring(0, 1)),
                Integer.parseInt(code.substring(1)));
    }

    // day L, module 2 -> "L2"
    public String toCode() {
        return day.name() + moduleNumber;
    }
}
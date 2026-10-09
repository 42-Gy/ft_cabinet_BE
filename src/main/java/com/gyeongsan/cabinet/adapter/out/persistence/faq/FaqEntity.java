package com.gyeongsan.cabinet.adapter.out.persistence.faq;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "FAQ")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "SEED_KEY", length = 60)
    private String seedKey;

    @Column(name = "CATEGORY", nullable = false, length = 30)
    private String category;

    @Column(name = "ANSWER", nullable = false, length = 2000)
    private String answer;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "faq",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<FaqQuestionEntity> questions = new ArrayList<>();

    public static FaqEntity create() {
        return new FaqEntity();
    }

    /** 질문 표현 목록을 통째로 교체한다(순서가 곧 대표 질문 순서). */
    public void replaceQuestions(List<String> texts) {
        questions.clear();
        for (int i = 0; i < texts.size(); i++) {
            questions.add(new FaqQuestionEntity(this, i, texts.get(i)));
        }
    }
}

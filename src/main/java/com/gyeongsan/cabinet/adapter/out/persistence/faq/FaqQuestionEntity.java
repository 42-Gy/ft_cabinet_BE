package com.gyeongsan.cabinet.adapter.out.persistence.faq;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "FAQ_QUESTION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FAQ_ID", nullable = false)
    private FaqEntity faq;

    @Column(name = "SORT_ORDER", nullable = false)
    private int sortOrder;

    @Column(name = "QUESTION", nullable = false, length = 200)
    private String question;

    FaqQuestionEntity(FaqEntity faq, int sortOrder, String question) {
        this.faq = faq;
        this.sortOrder = sortOrder;
        this.question = question;
    }
}

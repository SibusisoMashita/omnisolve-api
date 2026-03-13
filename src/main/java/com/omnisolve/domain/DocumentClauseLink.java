package com.omnisolve.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "document_clause_links")
@IdClass(DocumentClauseLink.DocumentClauseLinkId.class)
public class DocumentClauseLink {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clause_id", nullable = false)
    private Clause clause;

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Clause getClause() {
        return clause;
    }

    public void setClause(Clause clause) {
        this.clause = clause;
    }

    // Composite key class
    public static class DocumentClauseLinkId implements Serializable {
        private UUID document;
        private Long clause;

        public DocumentClauseLinkId() {
        }

        public DocumentClauseLinkId(UUID document, Long clause) {
            this.document = document;
            this.clause = clause;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DocumentClauseLinkId that = (DocumentClauseLinkId) o;
            return Objects.equals(document, that.document) && Objects.equals(clause, that.clause);
        }

        @Override
        public int hashCode() {
            return Objects.hash(document, clause);
        }
    }
}

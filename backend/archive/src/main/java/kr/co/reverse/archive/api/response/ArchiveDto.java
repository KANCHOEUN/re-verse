package kr.co.reverse.archive.api.response;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class ArchiveDto {

    private UUID archiveId;

    private UserRes owner;

    private String title;

    private String description;

    private Boolean bookmark = false;

    private UserRes member;

    @QueryProjection
    public ArchiveDto(UUID archiveId, UserRes owner, String title, String description, UserRes member) {
        this.archiveId = archiveId;
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.member = member;
    }
}

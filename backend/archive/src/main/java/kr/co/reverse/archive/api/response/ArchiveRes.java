package kr.co.reverse.archive.api.response;

import com.querydsl.core.annotations.QueryProjection;
import kr.co.reverse.archive.db.entity.Archive;
import kr.co.reverse.archive.db.entity.User;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ArchiveRes {

    private UUID archiveId;

    private UserRes owner;

    private String title;

    private String description;

    private Boolean bookmark;

    private List<UserRes> members;

    @QueryProjection
    public ArchiveRes(UUID archiveId, UserRes owner, String title, String description) {
        this.archiveId = archiveId;
        this.owner = owner;
        this.title = title;
        this.description = description;
    }

    public static ArchiveRes of(UUID archiveId, UserRes owner, String title, String description, boolean bookmark, List<UserRes> members) {
        ArchiveRes archiveRes = new ArchiveRes();
        archiveRes.setArchiveId(archiveId);
        archiveRes.setOwner(owner);
        archiveRes.setTitle(title);
        archiveRes.setDescription(description);
        archiveRes.setBookmark(bookmark);
        archiveRes.setMembers(members);
        return archiveRes;
    }
}

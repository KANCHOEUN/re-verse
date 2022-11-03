package kr.co.reverse.archive.api.service;

import kr.co.reverse.archive.api.request.ArchiveReq;
import kr.co.reverse.archive.api.response.ArchiveDetailRes;
import kr.co.reverse.archive.api.response.ArchiveRes;
import kr.co.reverse.archive.api.response.StuffRes;
import kr.co.reverse.archive.api.response.UserRes;
import kr.co.reverse.archive.db.entity.Archive;
import kr.co.reverse.archive.db.entity.ArchiveMember;
import kr.co.reverse.archive.db.entity.Role;
import kr.co.reverse.archive.db.entity.User;
import kr.co.reverse.archive.db.repository.ArchiveMemberRepository;
import kr.co.reverse.archive.db.repository.ArchiveRepository;
import kr.co.reverse.archive.db.repository.StuffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final ArchiveRepository archiveRepository;

    private final ArchiveMemberRepository archiveMemberRepository;

    private final StuffRepository stuffRepository;

    @Transactional
    public Archive createArchive(ArchiveReq archiveReq, User user) {
        String title = archiveReq.getTitle();
        String description = archiveReq.getDescription();

        if (title == null || description == null) {
            throw new IllegalArgumentException();
        }

        Archive archive = Archive.builder()
                .title(title)
                .description(description)
                .isDeleted(false)
                .ownerId(user.getId())
                .createdTime(LocalDateTime.now())
                .build();
        archiveRepository.save(archive);

        ArchiveMember archiveMember = ArchiveMember.builder()
                .archive(archive)
                .user(user)
                .role(Role.READ)
                .build();
        archiveMemberRepository.save(archiveMember);

        return archive;
    }

    public List<ArchiveRes> getArchives(User user) {
        List<ArchiveRes> myArchives = archiveRepository.getMyArchives(user.getId());

        if (myArchives != null) {
            for (ArchiveRes archiveRes : myArchives) {
                UUID archiveId = archiveRes.getArchiveId();
                List<UserRes> members = archiveRepository.getMembers(archiveId);
                Boolean isBookmark = archiveRepository.checkBookmark(archiveId, user.getId());

                archiveRes.setMembers(members);
                archiveRes.setBookmark(isBookmark);
            }
        }

        return myArchives;
    }

    public List<ArchiveRes> getFriendArchives(User user) {
        List<ArchiveRes> friendArchives = archiveRepository.getFriendArchives(user.getId());

        if (friendArchives != null) {
            for (ArchiveRes archiveRes : friendArchives) {
                UUID archiveId = archiveRes.getArchiveId();
                List<UserRes> members = archiveRepository.getMembers(archiveId);
                Boolean isBookmark = archiveRepository.checkBookmark(archiveId, user.getId());

                archiveRes.setMembers(members);
                archiveRes.setBookmark(isBookmark);
            }
        }

        return friendArchives;
    }


    public ArchiveDetailRes getArchiveDetail(UUID archiveId) {
        ArchiveDetailRes archiveDetailRes = archiveRepository.getArchiveDetail(archiveId);

        if (archiveDetailRes == null) {
            throw new NoSuchElementException();
        }

        List<StuffRes> stuffs = stuffRepository.getStuffs(archiveId);
        List<UserRes> members = archiveRepository.getMembers(archiveId);

        archiveDetailRes.setStuffs(stuffs);
        archiveDetailRes.setMembers(members);

        return archiveDetailRes;
    }

    public Archive getArchive(UUID archiveId) {
        return archiveRepository.findById(archiveId)
                .orElseThrow(() -> new NoSuchElementException());
    }

}

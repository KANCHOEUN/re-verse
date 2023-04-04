package kr.co.reverse.archive.db.repository;

import kr.co.reverse.archive.db.entity.ArchiveMember;
import kr.co.reverse.archive.db.entity.User;

import java.util.List;

public interface ArchiveMemberRepositoryCustom {

    List<ArchiveMember> getArchiveMembers(User user, User target);
}

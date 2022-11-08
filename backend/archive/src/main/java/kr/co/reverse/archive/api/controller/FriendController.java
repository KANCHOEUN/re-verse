package kr.co.reverse.archive.api.controller;

import kr.co.reverse.archive.api.request.ArchiveMemberReq;
import kr.co.reverse.archive.api.request.BookmarkReq;
import kr.co.reverse.archive.api.request.FriendInvitationReq;
import kr.co.reverse.archive.api.request.InvitationReplyReq;
import kr.co.reverse.archive.api.response.FriendInvitationRes;
import kr.co.reverse.archive.api.response.FriendInvitationsRes;
import kr.co.reverse.archive.api.response.FriendRes;
import kr.co.reverse.archive.api.response.FriendsRes;
import kr.co.reverse.archive.api.service.ArchiveService;
import kr.co.reverse.archive.api.service.FriendService;
import kr.co.reverse.archive.api.service.UserService;
import kr.co.reverse.archive.common.exception.NotFriendException;
import kr.co.reverse.archive.db.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    private final UserService userService;

    private final ArchiveService archiveService;

    @GetMapping
    public ResponseEntity<? extends FriendsRes> getFriends() {

        String userId = userService.getUserId();
        User user = userService.getPlayer(userId);

        List<FriendRes> myFriends = friendService.getFriends(user);
        return ResponseEntity.ok(FriendsRes.of(myFriends));

    }

    @PostMapping
    public ResponseEntity createFriendInvitation(@RequestBody FriendInvitationReq friendInvitationReq) {

        String userId = userService.getUserId();
        User user = userService.getPlayer(userId);
        User target = userService.getUserByNickname(friendInvitationReq.getNickname());

        // 2022-11-08
        // 이미 요청을 보낸 관계면 요청을 만들지 않는다.
        if (friendService.getFriendInvitationTo(user, target) != null) {

            return ResponseEntity.status(HttpStatus.CREATED).build();

        } else if (friendService.getFriendInvitationTo(target, user) != null) {
            // 유저와 타겟을 바꿔서 조회를 했는데 친구 요청이 이미 존재하면, 둘 다 서로에게 요청을 보낸 것이므로 친구 수락을 한뒤, 친구 요청을 1 row 지우고, 신규로 들어온 요청은 생성 X
            friendService.deleteFriendInvitation(target, user);
            friendService.createFriend(user, target);

        } else {

            friendService.createFriendInvitation(user, target);
        }


        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/reply")
    public ResponseEntity<? extends FriendInvitationsRes> getFriendInvitations() {

        String userId = userService.getUserId();
        User user = userService.getPlayer(userId);

        List<FriendInvitationRes> waitingTo = friendService.getFriendInvitationsTo(user);
        List<FriendInvitationRes> waitingFrom = friendService.getFriendInvitationsFrom(user);

        return ResponseEntity.ok(FriendInvitationsRes.of(waitingTo, waitingFrom));
    }

    @PostMapping("/reply")
    public ResponseEntity reply(@RequestBody InvitationReplyReq invitationReplyReq) {

        String userId = userService.getUserId();
        User user = userService.getPlayer(userId);
        User target = userService.getUserByNickname(invitationReplyReq.getNickname());

        Boolean isAccepted = invitationReplyReq.getIsAccepted();

        friendService.reply(user, target, isAccepted);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping
    public ResponseEntity delete(@RequestParam(name = "nickname") String nickname) {

        String userId = userService.getUserId();
        User user = userService.getPlayer(userId);
        User target = userService.getUserByNickname(nickname);

        friendService.deleteFriend(user, target);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/bookmark")
    public ResponseEntity createBookmark(@RequestBody BookmarkReq bookmarkReq) {

        UUID userId = null;
        User user = null;
        Archive archive = null;
//        User user = userService.getUser(userId);
//        Archive archive = archiveService.getArchive(bookmarkReq.getArchiveId());


        friendService.createBookmark(archive, user);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/bookmark/{archive_id}")
    public ResponseEntity deleteBookmark(@PathVariable(name = "archive_id") UUID archiveId) {

        String userId = userService.getUserId();
        User user = userService.getPlayer(userId);
        Archive archive = archiveService.getArchive(archiveId);

        friendService.deleteBookmark(archive, user);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/archive-member")
    public ResponseEntity createArchiveMember(@RequestBody ArchiveMemberReq archiveMemberReq) {

        String userId = userService.getUserId();
        User user = userService.getPlayer(userId);
        Archive archive = archiveService.getArchive(archiveMemberReq.getArchiveId());
        User target = userService.getUserByNickname(archiveMemberReq.getNickname());

        if (friendService.checkFriend(user, target)) {
            throw new NotFriendException();
        }
        friendService.createArchiveMember(archive, target, archiveMemberReq.getRole());

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/archive-member/{archive_id}")
    public ResponseEntity deleteArchiveMember(@PathVariable(name = "archive_id") UUID archiveId, @RequestParam(name = "nickname") String nickname) {

        String userId = userService.getUserId();
        User user = userService.getPlayer(userId);
        User target = userService.getUserByNickname(nickname);
        Archive archive = archiveService.getArchive(archiveId);

        if (friendService.checkFriend(user, target)) {
            throw new NotFriendException();
        }
        friendService.deleteArchiveMember(archive, target);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

}

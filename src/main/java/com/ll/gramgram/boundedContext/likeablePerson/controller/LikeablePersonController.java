package com.ll.gramgram.boundedContext.likeablePerson.controller;

import com.ll.gramgram.base.rq.Rq;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.service.LikeablePersonService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/likeablePerson")
@RequiredArgsConstructor
public class LikeablePersonController {
    private final Rq rq;
    private final LikeablePersonService likeablePersonService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/add")
    public String showAdd() {
        return "usr/likeablePerson/add";
    }

    @AllArgsConstructor
    @Getter
    public static class AddForm {
        private final String username;
        private final int attractiveTypeCode;
    }

    //케이스 4 : 한명의 인스타회원이 다른 인스타회원에게 중복으로 호감표시를 할 수 없습니다.
    //예를들어 본인의 인스타ID가 aaaa, 상대의 인스타ID가 bbbb 라고 하자.
    //aaaa 는 bbbb 에게 호감을 표시한다.(사유 : 외모)
    //잠시 후 aaaa 는 bbbb 에게 다시 호감을 표시한다.(사유 : 외모)
    //    # 어떠한 회원이 특정회원에 대해서 이미 호감표시를 했는지 검사하는 SQL, 질의결과가 하나라도 있다면 이미 호감을 표시한 경우이다.
    //    # 여기서 1은 로그인한 회원의 인스트 계정 번호이고
    //    # 여기서 2는 상대방의 인스타계정 번호이다.
    //    SELECT *
    //    FROM likeable_person
    //    WHERE from_insta_member_id = 1
    //    AND to_insta_member_id = 2;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/add")
    public String add(@Valid AddForm addForm) {
        InstaMember instaMember = rq.getMember().getInstaMember();
        List<LikeablePerson> likeablePeople = instaMember.getFromLikeablePeople();

        for(int i = 0; i <= likeablePeople.size(); i++){
            System.out.println("지금: "+ addForm.getUsername() + "기존: " +likeablePeople.get(i).getToInstaMemberUsername());
            if(addForm.getUsername() == likeablePeople.get(i).getToInstaMemberUsername()) {
                    return;
            }
        }
        RsData<LikeablePerson> createRsData = likeablePersonService.like(rq.getMember(), addForm.getUsername(), addForm.getAttractiveTypeCode());
        if (createRsData.isFail()) {
            return rq.historyBack(createRsData);
        }

        return rq.redirectWithMsg("/likeablePerson/list", createRsData);
    }



    @PreAuthorize("isAuthenticated()")
    @GetMapping("/list")
    public String showList(Model model) {
        InstaMember instaMember = rq.getMember().getInstaMember();

        // 인스타인증을 했는지 체크
        if (instaMember != null) {
            // 해당 인스타회원이 좋아하는 사람들 목록
            List<LikeablePerson> likeablePeople = instaMember.getFromLikeablePeople();
            model.addAttribute("likeablePeople", likeablePeople);
        }

        return "usr/likeablePerson/list";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("{id}")
    public String delete(@PathVariable Long id) {
        LikeablePerson likeablePerson = likeablePersonService.findById(id).orElse(null);

        RsData canActorDeleteRsData = likeablePersonService.canActorDelete(rq.getMember(), likeablePerson);

        if (canActorDeleteRsData.isFail()) return rq.historyBack(canActorDeleteRsData);

        RsData deleteRsData = likeablePersonService.delete(likeablePerson);

        if (deleteRsData.isFail()) return rq.historyBack(deleteRsData);

        return rq.redirectWithMsg("/likeablePerson/list", deleteRsData);
    }
}


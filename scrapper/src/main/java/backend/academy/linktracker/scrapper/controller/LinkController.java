package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.link.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.link.LinkResponse;
import backend.academy.linktracker.scrapper.dto.link.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.link.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.service.LinkService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @GetMapping
    public ResponseEntity<ListLinksResponse> getLinks(
        @RequestHeader("Tg-Chat-Id") long chatId
    ) {
        MDC.put("chatId", String.valueOf(chatId));
        try {
            log.atInfo().log("Getting links");
            List<LinkResponse> links = linkService.findAll(chatId).stream()
                .map(this::toResponse)
                .toList();
            return ResponseEntity.ok(new ListLinksResponse(links, links.size()));
        } finally {
            MDC.clear();
        }
    }

    @PostMapping
    public ResponseEntity<LinkResponse> addLink(
        @RequestHeader("Tg-Chat-Id") long chatId,
        @Valid @RequestBody AddLinkRequest request
    ) {
        MDC.put("chatId", String.valueOf(chatId));
        MDC.put("url", request.link().toString());
        try {
            log.atInfo().log("Adding link");
            Link link = linkService.add(chatId, request.link(), request.tags(), request.filters());
            return ResponseEntity.ok(toResponse(link));
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping
    public ResponseEntity<LinkResponse> removeLink(
        @RequestHeader("Tg-Chat-Id") long chatId,
        @Valid @RequestBody RemoveLinkRequest request
    ) {
        MDC.put("chatId", String.valueOf(chatId));
        MDC.put("url", request.link().toString());
        try {
            log.atInfo().log("Removing link");
            Link link = linkService.remove(chatId, request.link());
            return ResponseEntity.ok(toResponse(link));
        } finally {
            MDC.clear();
        }
    }

    private LinkResponse toResponse(Link link) {
        return new LinkResponse(link.getId(), link.getUrl(), link.getTags(), link.getFilters());
    }
}

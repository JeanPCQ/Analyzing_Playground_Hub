package com.stockpilot.wms.api;

import com.stockpilot.wms.api.ApiDtos.CreatePartRequest;
import com.stockpilot.wms.api.ApiDtos.PartDto;
import com.stockpilot.wms.domain.Part;
import com.stockpilot.wms.service.PartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
@Tag(name = "Parts")
public class PartApiController {

    private final PartService partService;

    public PartApiController(PartService partService) {
        this.partService = partService;
    }

    @GetMapping
    public List<PartDto> list(@RequestParam(required = false) String q) {
        return partService.search(q).stream().map(PartDto::from).toList();
    }

    @GetMapping("/{partNumber}")
    public PartDto get(@PathVariable String partNumber) {
        return PartDto.from(partService.summarize(partService.getByNumber(partNumber)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartDto create(@Valid @RequestBody CreatePartRequest request) {
        Part part = partService.create(request.partNumber(), request.description(),
                request.standardCost(), request.reorderPoint());
        return PartDto.from(partService.summarize(part));
    }
}

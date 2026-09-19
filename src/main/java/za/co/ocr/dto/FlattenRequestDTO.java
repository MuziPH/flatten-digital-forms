package za.co.ocr.dto;

import lombok.Builder;
import za.co.ocr.model.EmailInfo;
import za.co.ocr.model.OTTEmailInstruction;

import java.util.List;

@Builder
public record FlattenRequestDTO(
        String emailGuid,
        EmailInfo emailInfo,
        List<OTTEmailInstruction.Attachment> attachments
) {
}

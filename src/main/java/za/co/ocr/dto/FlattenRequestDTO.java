package za.co.ocr.dto;

import lombok.Builder;
import za.co.ocr.model.EmailInfo;
import za.co.ocr.model.OTTEmailInstruction;

import java.util.List;

/**
 * Carries everything needed to request a PDF flattening operation.
 *
 * <p>Records are a compact way to model data transfer objects. This request
 * groups the email identifier, the email metadata, and the attachments so the
 * flattening logic has all of the information it needs in one place.</p>
 */
@Builder
public record FlattenRequestDTO(
        String emailGuid,
        EmailInfo emailInfo,
        List<OTTEmailInstruction.Attachment> attachments
) {
}

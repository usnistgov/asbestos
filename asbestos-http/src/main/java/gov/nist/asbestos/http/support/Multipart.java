package gov.nist.asbestos.http.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 */
class Multipart {
    List<Part> parts = new ArrayList<>();
    String startPartId;

    Optional<Part> getStartPart() {
        if (startPartId != null && parts.size() > 0) {
            return  Optional.of(parts.stream()
                    .filter(p -> ! Optional.ofNullable(p.getId()).orElse("").isEmpty() && p.getId().equals(startPartId))
                    .findFirst()
                    .get());
        } else if (parts.size() > 0) {
            return Optional.of(parts.get(0));
        }
        return Optional.empty();
    }
}

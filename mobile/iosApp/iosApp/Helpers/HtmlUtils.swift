import Foundation

/// Lightweight HTML stripping and entity decoding for plain-text contexts.
/// Full HTML rendering uses WKWebView in the detail screen.
extension String {

    /// Decode HTML character entities (&amp; &lt; &#39; &#x27; etc.).
    func strippingHTMLEntities() -> String {
        var result = self
        // Named entities
        let namedEntities: [String: String] = [
            "&amp;": "&", "&lt;": "<", "&gt;": ">",
            "&quot;": "\"", "&apos;": "'", "&nbsp;": "\u{00A0}",
            "&ndash;": "\u{2013}", "&mdash;": "\u{2014}",
            "&laquo;": "\u{00AB}", "&raquo;": "\u{00BB}",
            "&bull;": "\u{2022}", "&hellip;": "\u{2026}",
            "&copy;": "\u{00A9}", "&reg;": "\u{00AE}",
            "&trade;": "\u{2122}", "&euro;": "\u{20AC}",
        ]
        for (entity, char) in namedEntities {
            result = result.replacingOccurrences(of: entity, with: char)
        }
        // Decimal numeric entities: &#123;
        if let regex = try? NSRegularExpression(pattern: "&#(\\d+);") {
            let range = NSRange(result.startIndex..., in: result)
            let matches = regex.matches(in: result, range: range).reversed()
            for match in matches {
                if let codeRange = Range(match.range(at: 1), in: result),
                   let codePoint = UInt32(result[codeRange]),
                   let scalar = Unicode.Scalar(codePoint) {
                    let fullRange = Range(match.range, in: result)!
                    result.replaceSubrange(fullRange, with: String(Character(scalar)))
                }
            }
        }
        // Hex numeric entities: &#x1F4A9;
        if let regex = try? NSRegularExpression(pattern: "&#x([0-9a-fA-F]+);") {
            let range = NSRange(result.startIndex..., in: result)
            let matches = regex.matches(in: result, range: range).reversed()
            for match in matches {
                if let codeRange = Range(match.range(at: 1), in: result),
                   let codePoint = UInt32(result[codeRange], radix: 16),
                   let scalar = Unicode.Scalar(codePoint) {
                    let fullRange = Range(match.range, in: result)!
                    result.replaceSubrange(fullRange, with: String(Character(scalar)))
                }
            }
        }
        return result
    }

    /// Strip all HTML tags and decode entities, returning plain text.
    /// Block-level tags are replaced with newlines.
    func strippingHTML() -> String {
        guard !isEmpty else { return "" }
        var text = self
        // Replace <br> with newline
        text = text.replacingOccurrences(
            of: "<br\\s*/?>",
            with: "\n",
            options: [.regularExpression, .caseInsensitive]
        )
        // Replace block-level closing/opening tags with newline
        text = text.replacingOccurrences(
            of: "</?(p|div|li|tr|h[1-6])[^>]*>",
            with: "\n",
            options: [.regularExpression, .caseInsensitive]
        )
        // Strip remaining tags
        text = text.replacingOccurrences(
            of: "<[^>]*>",
            with: "",
            options: .regularExpression
        )
        // Decode entities
        text = text.strippingHTMLEntities()
        // Collapse whitespace
        text = text.components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespaces)
                      .replacingOccurrences(of: "\\s{2,}", with: " ", options: .regularExpression) }
            .joined(separator: "\n")
        // Collapse multiple blank lines
        text = text.replacingOccurrences(of: "\n{3,}", with: "\n\n", options: .regularExpression)
        return text.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}


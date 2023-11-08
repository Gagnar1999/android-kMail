/*
 * Infomaniak Mail - Android
 * Copyright (C) 2023 Infomaniak Network SA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.infomaniak.mail.utils

import com.infomaniak.mail.data.models.message.Body
import io.sentry.Sentry
import io.sentry.SentryLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object MessageBodyUtils {

    const val INFOMANIAK_SIGNATURE_HTML_CLASS_NAME = "editorUserSignature"
    const val INFOMANIAK_REPLY_QUOTE_HTML_CLASS_NAME = "ik_mail_quote"
    const val INFOMANIAK_FORWARD_QUOTE_HTML_CLASS_NAME = "forwardContentMessage"

    private const val QUOTE_DETECTION_TIMEOUT = 1_500L

    private val quoteDescriptors = arrayOf(
        // Do not detect this quote as long as we can't detect siblings quotes or else a single reply will be missing among the
        // many replies of an Outlook reply "chain", which is worst than simply ignoring it.
        // "#divRplyFwdMsg", // Microsoft Outlook
        "blockquote[type=cite]", // macOS and iOS mail client
        "#isForwardContent",
        "#isReplyContent",
        "#mailcontent:not(table)",
        "#origbody",
        "#oriMsgHtmlSeperator",
        "#reply139content",
        anyCssClassContaining("gmail_extra"), // Gmail
        anyCssClassContaining("gmail_quote"), // Gmail
        anyCssClassContaining(INFOMANIAK_REPLY_QUOTE_HTML_CLASS_NAME), // That's us :3
        anyCssClassContaining("moz-cite-prefix"), // Mozilla Thunderbird
        anyCssClassContaining("protonmail_quote"), // Proton Mail
        anyCssClassContaining("yahoo_quoted"), // Yahoo! Mail
        anyCssClassContaining("zmail_extra"), // Zoho Mail
        "[name=\"quote\"]", // GMX
    )

    suspend fun splitContentAndQuote(body: Body): SplitBody {

        val bodyContent = body.value
        if (body.type == Utils.TEXT_PLAIN) return SplitBody(bodyContent)

        return Utils.executeWithTimeoutOrDefault(
            timeout = QUOTE_DETECTION_TIMEOUT,
            defaultValue = SplitBody(bodyContent),
            block = {
                val (content, quotes) = splitContentAndQuotes(htmlDocument = Jsoup.parse(bodyContent))
                if (quotes.isEmpty() || quotes.all { it.isBlank() }) SplitBody(bodyContent) else SplitBody(content, bodyContent)
            },
            onTimeout = {
                Sentry.withScope { scope ->
                    scope.level = SentryLevel.WARNING
                    scope.setExtra("body size", "${bodyContent.toByteArray().size} bytes")
                    scope.setExtra("email", AccountUtils.currentMailboxEmail.toString())
                    Sentry.captureMessage("Timeout reached while displaying a Message's body")
                }
            },
        )
    }

    private fun CoroutineScope.splitContentAndQuotes(htmlDocument: Document): Pair<String, MutableList<String>> {
        val quotes = mutableListOf<String>()

        for (quoteDescriptor in quoteDescriptors) {
            ensureActive()

            val foundQuotes = htmlDocument.select(quoteDescriptor)
            if (foundQuotes.isNotEmpty()) {
                foundQuotes.forEach { foundQuote ->
                    quotes.add(foundQuote.outerHtml())
                    foundQuote.remove()
                }
            }
        }

        return htmlDocument.outerHtml() to quotes
    }

    //region Utils
    /**
     * Some Email clients rename CSS classes to prefix them.
     * We match all the CSS classes that contain the quote, in case this one has been renamed.
     * @return a new CSS query
     */
    private fun anyCssClassContaining(cssClass: String) = "[class*=$cssClass]"
    //endregion

    data class SplitBody(
        val content: String,
        val quote: String? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            return other === this || (other is SplitBody && other.content == content && other.quote == quote)
        }

        override fun hashCode(): Int = 31 * content.hashCode() + quote.hashCode()
    }
}

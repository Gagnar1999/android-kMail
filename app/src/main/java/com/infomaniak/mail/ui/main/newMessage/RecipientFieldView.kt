/*
 * Infomaniak kMail - Android
 * Copyright (C) 2022-2023 Infomaniak Network SA
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
package com.infomaniak.mail.ui.main.newMessage

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.infomaniak.lib.core.utils.getAttributes
import com.infomaniak.lib.core.utils.hideKeyboard
import com.infomaniak.lib.core.utils.showKeyboard
import com.infomaniak.mail.MatomoMail.trackMessageEvent
import com.infomaniak.mail.R
import com.infomaniak.mail.data.models.correspondent.MergedContact
import com.infomaniak.mail.data.models.correspondent.Recipient
import com.infomaniak.mail.databinding.ViewContactChipContextMenuBinding
import com.infomaniak.mail.databinding.ViewRecipientFieldBinding
import com.infomaniak.mail.utils.isEmail
import com.infomaniak.mail.utils.toggleChevron
import kotlin.math.min

class RecipientFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewRecipientFieldBinding.inflate(LayoutInflater.from(context), this, true) }
    private var contactAdapter: ContactAdapter
    private var contactChipAdapter: ContactChipAdapter

    private var contactMap: Map<String, Map<String, MergedContact>> = emptyMap()
    private lateinit var popupRecipient: Recipient
    private var popupDeletesTheCollapsedChip = false

    private val popupMaxWidth by lazy { resources.getDimensionPixelSize(R.dimen.contactPopupMaxWidth) }

    private val contextMenuBinding by lazy {
        ViewContactChipContextMenuBinding.inflate(LayoutInflater.from(context), null, false)
    }

    private val contactPopupWindow by lazy {
        PopupWindow(context).apply {
            contentView = contextMenuBinding.root
            height = ViewGroup.LayoutParams.WRAP_CONTENT

            val displayMetrics = context.resources.displayMetrics
            val percentageOfScreen = (displayMetrics.widthPixels * MAX_WIDTH_PERCENTAGE).toInt()
            width = min(percentageOfScreen, popupMaxWidth)

            isFocusable = true
        }
    }

    private var onAutoCompletionToggled: ((hasOpened: Boolean) -> Unit)? = null
    private var onToggleEverything: ((isCollapsed: Boolean) -> Unit)? = null
    private var onContactRemoved: ((Recipient) -> Unit)? = null
    private var onContactAdded: ((Recipient) -> Unit)? = null
    private var onCopyContactAddress: ((Recipient) -> Unit)? = null
    private var gotFocus: (() -> Unit)? = null
    private var setSnackBar: ((Int) -> Unit) = {}

    private var canCollapseEverything = false
    private var otherFieldsAreAllEmpty = true
    private var isEverythingCollapsed = true
        set(value) {
            field = value
            isSelfCollapsed = field
            updateCollapsedEverythingUiState(value)
        }
    private var isSelfCollapsed = true
        set(value) {
            if (value == field) return
            field = value
            updateCollapsedUiState(value)
        }

    private lateinit var autoCompletedContacts: RecyclerView

    private var isAutoCompletionOpened
        get() = autoCompletedContacts.isVisible
        set(value) {
            autoCompletedContacts.isVisible = value
            binding.chevron.isGone = value || !shouldDisplayChevron()
        }

    init {
        with(binding) {
            attrs?.getAttributes(context, R.styleable.RecipientFieldView) {
                prefix.text = getText(R.styleable.RecipientFieldView_title)
                canCollapseEverything = getBoolean(R.styleable.RecipientFieldView_canCollapseEverything, canCollapseEverything)
            }

            contactAdapter = ContactAdapter(
                usedContacts = mutableSetOf(),
                onContactClicked = { addRecipient(it.email, it.name) },
                onAddUnrecognizedContact = {
                    val input = textInput.text.toString()
                    if (input.isEmail()) {
                        addRecipient(email = input, name = input)
                    } else {
                        setSnackBar(R.string.addUnknownRecipientInvalidEmail)
                    }
                },
                setSnackBar = { setSnackBar(it) },
            )

            contactChipAdapter = ContactChipAdapter(
                openContextMenu = ::showContactContextMenu,
                onBackspace = { recipient ->
                    removeRecipient(recipient)
                    focusTextField()
                }
            )

            chevron.isVisible = canCollapseEverything
            isSelfCollapsed = canCollapseEverything

            setupChipsRecyclerView()

            setToggleRelatedListeners()
            setTextInputListeners()
            setPopupMenuListeners()

            if (isInEditMode) {
                singleChip.root.isVisible = canCollapseEverything
                plusChip.isVisible = canCollapseEverything
            }
        }
    }

    private fun setupChipsRecyclerView() = with(binding) {
        chipsRecyclerView.adapter = contactChipAdapter

        (chipsRecyclerView.layoutManager as FlexboxLayoutManager).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
        }
    }

    private fun setToggleRelatedListeners() = with(binding) {
        if (canCollapseEverything) chevron.setOnClickListener {
            context.trackMessageEvent("openRecipientsFields", isSelfCollapsed)
            isEverythingCollapsed = !isEverythingCollapsed
            if (isSelfCollapsed) textInput.hideKeyboard()
        }

        plusChip.setOnClickListener {
            expand()
            textInput.showKeyboard()
        }

        transparentButton.setOnClickListener {
            expand()
            textInput.showKeyboard()
        }

        singleChip.root.setOnClickListener {
            showContactContextMenu(contactChipAdapter.getRecipients().first(), singleChip.root, true)
        }
    }

    private fun setTextInputListeners() = with(binding.textInput) {

        fun performContactSearch(text: CharSequence) {
            if ((text.trim().count()) > 0) {
                contactAdapter.filterField(text)
            } else {
                contactAdapter.clear()
            }
        }

        doOnTextChanged { text, _, _, _ ->
            if (text?.isNotEmpty() == true) {
                performContactSearch(text)
                if (!isAutoCompletionOpened) openAutoCompletion()
            } else if (isAutoCompletionOpened) {
                closeAutoCompletion()
            }
        }

        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && text?.isNotBlank() == true) {
                contactAdapter.addFirstAvailableItem()
            }
            true // Keep keyboard open
        }

        setBackspaceOnEmptyFieldListener(::focusLastChip)

        setOnFocusChangeListener { _, hasFocus -> if (hasFocus) gotFocus?.invoke() }
    }

    private fun setPopupMenuListeners() {
        contextMenuBinding.copyContactAddressButton.setOnClickListener {
            onCopyContactAddress?.invoke(popupRecipient)
            contactPopupWindow.dismiss()
        }

        contextMenuBinding.deleteContactButton.setOnClickListener {
            removeRecipient(popupRecipient)
            if (popupDeletesTheCollapsedChip) {
                popupDeletesTheCollapsedChip = false
                updateCollapsedChipValues(true)
            }
            contactPopupWindow.dismiss()
        }
    }

    private fun focusLastChip() {
        val count = contactChipAdapter.itemCount
        // chipsRecyclerView.children.last() won't work because they are not always ordered correctly
        if (count > 0) binding.chipsRecyclerView.getChildAt(count - 1).requestFocusFromTouch()
    }

    private fun focusTextField() {
        binding.textInput.requestFocus()
    }

    private fun updateCollapsedEverythingUiState(isEverythingCollapsed: Boolean) = with(binding) {
        chevron.toggleChevron(isEverythingCollapsed)
        onToggleEverything?.invoke(isEverythingCollapsed)
    }

    private fun updateCollapsedUiState(isCollapsed: Boolean) = with(binding) {
        updateCollapsedChipValues(isCollapsed)
        chipsRecyclerView.isGone = isCollapsed
    }

    private fun updateCollapsedChipValues(isCollapsed: Boolean) = with(binding) {
        val isTextInputAccessible = !isCollapsed || contactChipAdapter.isEmpty()

        singleChip.root.apply {
            isGone = isTextInputAccessible
            text = contactChipAdapter.getRecipients().firstOrNull()?.getNameOrEmail() ?: ""
        }
        plusChip.apply {
            isGone = !isCollapsed || contactChipAdapter.itemCount <= 1
            text = "+${contactChipAdapter.itemCount - 1}"
        }

        transparentButton.isGone = isTextInputAccessible
        textInput.isVisible = isTextInputAccessible
    }

    fun updateContacts(allContacts: List<MergedContact>, newContactMap: Map<String, Map<String, MergedContact>>) {
        contactAdapter.updateContacts(allContacts)
        contactMap = newContactMap
    }

    private fun openAutoCompletion() {
        isAutoCompletionOpened = true
        onAutoCompletionToggled?.invoke(isAutoCompletionOpened)
    }

    private fun closeAutoCompletion() {
        isAutoCompletionOpened = false
        onAutoCompletionToggled?.invoke(isAutoCompletionOpened)
    }

    private fun addRecipient(email: String, name: String) {
        if (contactChipAdapter.itemCount > MAX_ALLOWED_RECIPIENT) {
            setSnackBar(R.string.tooManyRecipients)
            return
        }

        if (contactChipAdapter.isEmpty()) expand()
        val recipient = Recipient().initLocalValues(email, name)
        val recipientIsNew = contactAdapter.addUsedContact(email)
        if (recipientIsNew) {
            contactChipAdapter.addChip(recipient)
            onContactAdded?.invoke(recipient)
            clearField()
        }
    }

    private fun showContactContextMenu(recipient: Recipient, anchor: BackspaceAwareChip, isForSingleChip: Boolean = false) {
        contextMenuBinding.contactDetails.setRecipient(recipient, contactMap)

        popupRecipient = recipient
        popupDeletesTheCollapsedChip = isForSingleChip

        hideKeyboard()
        contactPopupWindow.showAsDropDown(anchor)
    }

    private fun removeRecipient(recipient: Recipient) {
        val successfullyRemoved = contactAdapter.removeUsedEmail(recipient.email)
        if (successfullyRemoved) {
            contactChipAdapter.removeChip(recipient)
            onContactRemoved?.invoke(recipient)
        }
    }

    fun initRecipientField(
        autoComplete: RecyclerView,
        onAutoCompletionToggledCallback: (hasOpened: Boolean) -> Unit,
        onContactAddedCallback: ((Recipient) -> Unit),
        onContactRemovedCallback: ((Recipient) -> Unit),
        onCopyContactAddressCallback: ((Recipient) -> Unit),
        gotFocusCallback: (() -> Unit),
        onToggleEverythingCallback: ((isCollapsed: Boolean) -> Unit)? = null,
        setSnackBarCallback: (titleRes: Int) -> Unit,
    ) {
        autoCompletedContacts = autoComplete
        autoCompletedContacts.adapter = contactAdapter

        onToggleEverything = onToggleEverythingCallback
        onAutoCompletionToggled = onAutoCompletionToggledCallback
        onContactAdded = onContactAddedCallback
        onContactRemoved = onContactRemovedCallback
        onCopyContactAddress = onCopyContactAddressCallback

        gotFocus = gotFocusCallback

        setSnackBar = setSnackBarCallback
    }

    fun clearField() {
        binding.textInput.setText("")
    }

    fun initRecipients(initialRecipients: List<Recipient>) {
        initialRecipients.forEach {
            if (contactChipAdapter.addChip(it)) {
                contactAdapter.addUsedContact(it.email)
            }
        }
        updateCollapsedChipValues(isSelfCollapsed)
    }

    fun collapse() {
        isSelfCollapsed = true
    }

    fun collapseEverything() {
        isEverythingCollapsed = true
    }

    private fun expand() {
        if (canCollapseEverything) isEverythingCollapsed = false else isSelfCollapsed = false
    }

    fun updateOtherFieldsVisibility(otherFieldsAreAllEmpty: Boolean) {
        this.otherFieldsAreAllEmpty = otherFieldsAreAllEmpty
        binding.chevron.isVisible = otherFieldsAreAllEmpty
    }

    private fun shouldDisplayChevron(): Boolean {
        return canCollapseEverything && otherFieldsAreAllEmpty
    }

    private companion object {
        const val MAX_WIDTH_PERCENTAGE = 0.8
        const val MAX_ALLOWED_RECIPIENT = 99
    }
}

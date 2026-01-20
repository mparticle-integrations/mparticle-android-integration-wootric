package com.mparticle.kits

import android.content.Context
import com.mparticle.MParticle.IdentityType
import com.mparticle.kits.KitIntegration.AttributeListener
import com.wootric.androidsdk.Wootric

class WootricKit :
    KitIntegration(),
    AttributeListener {
    private var endUserEmail: String? = null
    private var endUserCustomerId: String? = null
    private var endUserProperties: HashMap<String, String?>? = null
    private var wootric: Wootric? = null

    override fun getInstance(): Wootric? {
        val activityWeakReference = currentActivity
        if (activityWeakReference != null) {
            val activity = activityWeakReference.get()
            if (activity != null) {
                wootric =
                    Wootric.init(
                        activity,
                        settings[CLIENT_ID],
                        settings[ACCOUNT_TOKEN],
                    )
                wootric?.setProperties(endUserProperties)
                setWootricIdentity(wootric)
            }
        }

        return wootric
    }

    override fun getName(): String = KIT_NAME

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        // it's important the Wootric is not initialized until the hosting app calls getInstance, with
        // the correct Activity
        require(
            !(
                KitUtils.isEmpty(settings[CLIENT_ID]) ||
                    KitUtils.isEmpty(CLIENT_SECRET) ||
                    KitUtils.isEmpty(ACCOUNT_TOKEN)
            ),
        ) { WOOTRIC_MISSING_REQUIRED_SETTINGS_MESSAGE }
        return emptyList()
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> = emptyList()

    override fun setUserIdentity(
        identityType: IdentityType,
        id: String,
    ) {
        if (IdentityType.Email == identityType) {
            endUserEmail = id
        } else if (IdentityType.CustomerId == identityType) {
            endUserCustomerId = id
        }
        if (wootric != null) {
            setWootricIdentity(wootric)
        }
    }

    override fun removeUserIdentity(identityType: IdentityType) {
        if (IdentityType.Email == identityType) {
            endUserEmail = null
        } else if (IdentityType.CustomerId == identityType) {
            endUserCustomerId = null
        }
        if (wootric != null) {
            setWootricIdentity(wootric)
        }
    }

    override fun logout(): List<ReportingMessage> = emptyList()

    private fun setWootricIdentity(wootric: Wootric?) {
        val endUserEmailSet = !endUserEmail.isNullOrEmpty()
        val endUserIdentifier = if (endUserEmailSet) endUserEmail else endUserCustomerId
        wootric?.setEndUserEmail(endUserIdentifier)
    }

    override fun setUserAttribute(
        key: String,
        value: String,
    ) {
        prepareEndUserProperties(key, value)
        if (wootric != null) {
            wootric?.setProperties(endUserProperties)
        }
    }

    override fun setUserAttributeList(
        s: String,
        list: List<String>,
    ) {}

    override fun supportsAttributeLists(): Boolean = false

    override fun setAllUserAttributes(
        attributes: Map<String, String>,
        attributeLists: Map<String, List<String>>,
    ) {
        endUserProperties = HashMap()
        for ((key, value) in attributes) {
            prepareEndUserProperties(key, value)
        }
        if (wootric != null) {
            wootric?.setProperties(endUserProperties)
        }
    }

    override fun removeUserAttribute(key: String) {
        if (wootric != null &&
            endUserProperties != null &&
            endUserProperties?.remove(
                KitUtils.sanitizeAttributeKey(
                    key,
                ),
            ) != null
        ) {
            wootric?.setProperties(endUserProperties)
        }
    }

    private fun prepareEndUserProperties(
        key: String,
        value: String,
    ) {
        endUserProperties?.let { it[KitUtils.sanitizeAttributeKey(key)] = value }
        if (endUserProperties == null) {
            endUserProperties = HashMap()
        }
    }

    companion object {
        private const val CLIENT_ID = "clientId"
        private const val CLIENT_SECRET = "clientSecret"
        private const val ACCOUNT_TOKEN = "accountToken"
        private const val KIT_NAME = "Wootric"
        private const val WOOTRIC_MISSING_REQUIRED_SETTINGS_MESSAGE =
            "Wootric missing required settings and will not start."
    }
}

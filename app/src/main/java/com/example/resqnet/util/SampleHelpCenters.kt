package com.example.resqnet.util

import com.example.resqnet.data.model.HelpCenter

object SampleHelpCenters {
    val centers = listOf(
        HelpCenter(
            name = "City Trauma Hospital",
            type = "Hospital",
            latitude = 12.9724,
            longitude = 77.5908,
            contact = "+91 080 2211 0099",
            notes = "Trauma, ICU, and emergency surgery"
        ),
        HelpCenter(
            name = "Harbor Relief Shelter",
            type = "Shelter",
            latitude = 12.9481,
            longitude = 77.5815,
            contact = "+91 080 2200 1188",
            notes = "Food, beds, family reunification desk"
        ),
        HelpCenter(
            name = "North Response Base",
            type = "Emergency Center",
            latitude = 12.9873,
            longitude = 77.6121,
            contact = "+91 080 2299 4433",
            notes = "Boat rescue, debris removal, and comms relay"
        ),
        HelpCenter(
            name = "Metro Medical Camp",
            type = "Hospital",
            latitude = 12.9618,
            longitude = 77.6362,
            contact = "+91 080 2121 8787",
            notes = "First aid, triage, pediatric support"
        )
    )
}

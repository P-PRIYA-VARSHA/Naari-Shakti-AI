package com.example.shaktibotprobono

import android.app.Application
import android.content.Context

object Utils {
    lateinit var appContext: Context
        private set

    fun init(app: Application) { appContext = app.applicationContext }
    
    fun getAllIndianStates(): List<String> {
        return listOf(
            "All",
            "Andhra Pradesh",
            "Arunachal Pradesh", 
            "Assam",
            "Bihar",
            "Chhattisgarh",
            "Goa",
            "Gujarat",
            "Haryana",
            "Himachal Pradesh",
            "Jharkhand",
            "Karnataka",
            "Kerala",
            "Madhya Pradesh",
            "Maharashtra",
            "Manipur",
            "Meghalaya",
            "Mizoram",
            "Nagaland",
            "Odisha",
            "Punjab",
            "Rajasthan",
            "Sikkim",
            "Tamil Nadu",
            "Telangana",
            "Tripura",
            "Uttar Pradesh",
            "Uttarakhand",
            "West Bengal",
            "Delhi",
            "Jammu and Kashmir",
            "Ladakh",
            "Chandigarh",
            "Dadra and Nagar Haveli and Daman and Diu",
            "Lakshadweep",
            "Puducherry",
            "Andaman and Nicobar Islands"
        )
    }
    
    fun getAllSpecializations(): List<String> {
        return listOf(
            "All",
            "Criminal Law",
            "Civil Law",
            "Family Law",
            "Corporate Law",
            "Constitutional Law",
            "Tax Law",
            "Property Law",
            "Labor Law",
            "Environmental Law",
            "Intellectual Property Law",
            "Banking Law",
            "Insurance Law",
            "Real Estate Law",
            "Immigration Law",
            "Human Rights Law",
            "Consumer Protection Law",
            "Cyber Law",
            "Media Law",
            "Sports Law",
            "Healthcare Law",
            "Education Law",
            "Agricultural Law",
            "Maritime Law",
            "Aviation Law"
        )
    }
    
    fun getAllLanguages(): List<String> {
        return listOf(
            "All",
            "English",
            "Hindi",
            "Bengali",
            "Telugu",
            "Marathi",
            "Tamil",
            "Gujarati",
            "Kannada",
            "Malayalam",
            "Punjabi",
            "Odia",
            "Assamese",
            "Sanskrit",
            "Urdu",
            "Kashmiri",
            "Sindhi",
            "Konkani",
            "Manipuri",
            "Nepali",
            "Bodo",
            "Santhali",
            "Dogri",
            "Maithili"
        )
    }
} 
package com.example.secureshe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavController
import com.example.secureshe.R
import kotlinx.coroutines.launch
import com.example.secureshe.ui.components.LocalDrawerController

@Composable
fun HelpScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isSystemInDarkTheme())
                    Brush.verticalGradient(listOf(Color(0xFF201A2E), Color(0xFF2A2240)))
                else
                    Brush.verticalGradient(listOf(Color(0xFFF8F5FF), Color(0xFFEDE3FF)))
            )
            .verticalScroll(rememberScrollState())
    ) {
        // Header: Hamburger + "Help" title (left), Logo (right)
        val drawer = LocalDrawerController.current
        HelpHeader(
            onMenuClick = { drawer.open() }
        )

        // Contact Support Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1E4FF)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Contact App Support:",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Spacer(Modifier.height(8.dp))
                Text("📩 Need help? Reach out to our support team anytime.", color = Color(0xFF1A1A1A), fontSize = 16.sp)
                Text("🤝 We’re here to assist you with any issues or feedback.", color = Color(0xFF1A1A1A), fontSize = 16.sp)
                Text("🔒 Your safety and experience are our top priority", color = Color(0xFF1A1A1A), fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text("📧 Email: shaktibot.naariai@gmail.com", color = Color(0xFF1A1A1A), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        // FAQ Section
        Text(
            text = "FAQ's:",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        // Q1
        FAQItem(
            question = "Q.  What happens after I press SOS?",
            answer = "✅ When you press the shield SOS button, the app will:\n" +
                    "• Instantly send your live location to your saved emergency contacts.\n" +
                    "• Trigger alerts (like SMS/call/notification) depending on your settings.\n" +
                    "• Optionally notify local helpline numbers (if enabled).\n" +
                    "• Keep tracking your movement until you are safe."
        )

        // Q2
        FAQItem(
            question = "Q. How do I update my emergency contacts?",
            answer = "✅ Go to Profile → Emergency Contacts.\n" +
                    "• You can add, edit, or delete contacts anytime.\n" +
                    "• The updated list will be used the next time you trigger SOS."
        )

        // Q3
        FAQItem(
            question = "Q. Is my location shared with anyone other than my chosen contacts?",
            answer = "✅ No.\n" +
                    "• Your location is only shared with your saved emergency contacts and (optionally) police helpline if you enable it in Privacy settings.\n" +
                    "• We do not store or sell your location data. Your privacy and safety are our top priority."
        )

        // Q4
        FAQItem(
            question = "Q: Can I chat or contact a lawyer directly in the app?",
            answer = "✅ No.\n" +
                    "You cannot directly chat with a lawyer within the app.\n" +
                    "However, in the Pro Bono section, we have provided verified email IDs and phone numbers of lawyers for further contact and assistance."
        )

        // Q5
        FAQItem(
            question = "Q: Will anyone other than me have access to my chatbot conversations?",
            answer = "✅ No.\n" +
                    "Your chatbot history is private and secure.\n" +
                    "The chats will not be visible to anyone else and are accessible only to you."
        )

        // Feedback Section
        Spacer(Modifier.height(8.dp))
        FeedbackSection()

        // Footer
        Spacer(Modifier.height(24.dp))
        HelpFooter()
    }
}

@Composable
private fun FeedbackSection() {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1E4FF)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Feedback form:",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your voice matters! Let us know how we can make the app better. Got an idea or found an issue? Share your feedback here.",
                color = Color(0xFF1A1A1A),
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "https://forms.gle/DDQHk2uXB3JwCW6m9",
                color = colorResource(id = R.color.header_color),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://forms.gle/DDQHk2uXB3JwCW6m9")
                }
            )
        }
    }
}

@Composable
private fun HelpHeader(onMenuClick: () -> Unit) {
    // Use a calm header (lavender/blue gradient or solid)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.header_color))
            .height(80.dp)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Help",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun FAQItem(question: String, answer: String) {
    var expanded = remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1E4FF)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .clickable { expanded.value = !expanded.value },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { expanded.value = !expanded.value }) {
                    Icon(
                        imageVector = if (expanded.value) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded.value) "Collapse" else "Expand",
                        tint = colorResource(id = R.color.header_color)
                    )
                }
            }
            if (expanded.value) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = answer,
                    color = Color(0xFF1A1A1A),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun HelpFooter() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(id = R.color.header_color))
                .padding(vertical = 28.dp)
                .heightIn(min = 140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\"Help Is Always Here~Just A Click Away\"",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 0.6.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        // White strip under footer to account for system nav
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(Color.White)
        )
    }
}

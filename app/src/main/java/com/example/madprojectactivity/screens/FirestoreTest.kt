import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.madprojectactivity.data.model.UserProfile
import com.example.madprojectactivity.data.repository.UserRepository

@Composable
fun FirestoreTest() {
    val repo = remember { UserRepository() }

    Button(onClick = {
        repo.addUser(UserProfile(name = "Alice", email = "alice@example.com"))
    }) {
        Text("Add test user to Firestore")
    }
}

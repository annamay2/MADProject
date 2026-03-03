import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.madprojectactivity.data.model.UserProfile
import com.example.madprojectactivity.data.repository.UserRepository

@Composable
fun FirestoreTest() {
    val repo = remember { UserRepository() }
    var user by remember { mutableStateOf<UserProfile?>(null) }

    Column {

        Button(onClick = {
            repo.addUser(
                UserProfile(
                    name = "Anna",
                    email = "anna@example.com"
                )
            )
        }) {
            Text("Add test user to Firestore")
        }

        Button(onClick = {
            repo.getUser("PfmGY7NMJUR0rpY3UqPF") { result ->
                user = result
            }
        }) {
            Text("Get User")
        }
        var displayName = "x"
        var displayEmail = "x"
        if(user !== null){
            displayName = user!!.name
            displayEmail = user!!.email
        }


        Text("Name: ${displayName}")
        Text("Email: ${displayEmail}")
    }
}


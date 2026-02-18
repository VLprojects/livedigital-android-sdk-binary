package space.livedigital.example.calls.use_cases

import space.livedigital.example.calls.repositories.ContactsRepository
import space.livedigital.example.calls.repositories.HasContactResult

class HasContactUseCase(
    private val repository: ContactsRepository
) {

    suspend operator fun invoke(number: String): HasContactResult {
        return repository.hasContact(number)
    }
}
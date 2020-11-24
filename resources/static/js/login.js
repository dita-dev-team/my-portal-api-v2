function sendToBackend(idToken) {
    const form = $(document.createElement('form'));
    // $(form).attr("action", "/app/login");
    $(form).attr("method", "POST");
    const input = $("<input>").attr("type", "hidden").attr("name", "id_token").val(idToken);
    $(form).append($(input));
    form.appendTo(document.body)
    $(form).submit();
}

const unsubscribe = firebase.auth().onAuthStateChanged(async (firebaseUser) => {
    console.log('user state changed')
    unsubscribe()
    if (firebaseUser) {
        try {
            const idToken = await firebaseUser.getIdToken()
            sendToBackend(idToken)
        } catch (error) {
            console.error(error)
        }
    } else {
        console.log('User has signed out')
    }
});

async function login() {
    const provider = new firebase.auth.GoogleAuthProvider()
    try {
        const result = await firebase.auth().signInWithPopup(provider)
        sendToBackend(credential.idToken)
        const credential = firebase.auth.GoogleAuthProvider.credential(result.credential.idToken)
    } catch (error) {
        console.error(error)
    }
}
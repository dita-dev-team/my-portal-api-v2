function logout() {
    firebase.auth().signOut().then(function () {
        window.location = '/app/logout'
    }).catch(function (error) {
        console.error(error)
    });
}
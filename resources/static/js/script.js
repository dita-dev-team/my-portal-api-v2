const app = new Vue({
    el: '#app',
    data() {
        return {
            isLoading: false,
            examFile: null
        }
    },
    created() {
        const self = this
        // Only initialize state listener on login page
        if (window.location.pathname === '/app/login') {
            firebase.auth().onAuthStateChanged(async (firebaseUser) => {
                console.log('user state changed')
                // unsubscribe()
                if (firebaseUser) {
                    try {
                        const idToken = await firebaseUser.getIdToken()
                        await self.sendTokenToBackend(idToken)
                    } catch (error) {
                        console.error(error)
                    }
                } else {
                    console.log('User has signed out')
                }
            });
        }
    },
    methods: {
        async login() {
            this.isLoading = true
            const provider = new firebase.auth.GoogleAuthProvider()
            try {
                const result = await firebase.auth().signInWithPopup(provider)
            } catch (error) {
                console.error(error)
            }
        },
        async sendTokenToBackend(idToken) {
            const params = new URLSearchParams()
            params.append('id_token', idToken)
            try {
                const res = await axios.post(window.location.pathname, params, {
                    headers: {
                        'X-My-Portal': ''
                    }
                })
                if (res.status === 200 || res.status === 302) {
                    location.reload()
                }
            } catch (e) {
                console.error(e)
            }
        },
        logout() {
            firebase.auth().signOut().then(function () {
                window.location = '/app/logout'
            }).catch(function (error) {
                console.error(error)
            });
        },
        async clearExamSchedule() {
            const ret = confirm("Are you sure you want to clear the exam schedule?")
            if (ret === true) {
                this.isLoading = true
                try {
                    await axios.post(`${window.location.pathname}/delete`, {}, {
                        headers: {
                            'X-My-Portal': ''
                        }
                    })
                    this.$bvToast.toast(`Schedule cleared successfully`, {
                        autoHideDelay: 5000,
                        appendToast: false
                    })
                    location.reload()
                } catch (e) {
                    console.error(e)
                    this.$bvToast.toast(`Failed to clear schedule`, {
                        autoHideDelay: 5000,
                        appendToast: false
                    })
                } finally {
                    this.isLoading = false
                }

            }
        },
        async uploadSchedule() {
            this.isLoading = true
            try {
                const formData = new FormData()
                formData.append("file", this.examFile)
                await axios.post(`${window.location.pathname}/upload`, formData, {
                    headers: {
                        'Content-Type': 'multipart/form-data',
                        'X-My-Portal': ''
                    }
                })
                this.$bvToast.toast(`Exam schedule uploaded successfully`, {
                    autoHideDelay: 5000,
                    appendToast: false
                })
                location.reload()
            } catch (e) {
                console.error(e)
                this.$bvToast.toast(`Failed to upload exam schedule`, {
                    autoHideDelay: 5000,
                    appendToast: false
                })
            } finally {
                this.isLoading = false
            }
        }
    }
})
var photosList = source.photos;
var photos = [];

function getType(photoUrl) {
    var test = photoUrl.split("/").reverse()[0];
    switch (test) {
    case "F":
        return "photo";
    case "T":
        return "thumbnail";
    default:
        return "no-type";
    }
}

var i = 0;
for (i = 0; i < photosList.length; i += 1) {
    photos.push({value: photosList[i], type : getType(photosList[i])});
}
photos;

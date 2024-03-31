<?php
//header('Content-Type: application/x-www-form-urlencoded');

if ($_SERVER['REQUEST_METHOD'] != 'POST')
{
    exit();
}

error_reporting(E_ALL & ~E_DEPRECATED & ~E_STRICT);
//ini_set('display_errors', 0);

if(isset($_POST['debug']))
{
    error_reporting(E_ALL);
    ini_set('display_errors', 1);
}

require '../../config.php';
require_once '../../obfuscation.php';

if (STORAGE_METHOD === 'MYSQL')
{
    require_once '../../MySQL.php';
}
else
{
    require_once '../../json.php';
}

function create_account($email, $pass): string
{
    if (account_exists($email))
    {
        return 'Result=Failure&Reason=taken';
    }

    $account = ['email' => $email,
      'pass' => password_hash($pass, PASSWORD_DEFAULT)
    ];
    create_new_account($account);
    return load_account($email, $pass);
}

function load_account($email, $pass): string
{
    if (authenticate_account($email, $pass))
    {
        return 'Result=Success&Reason=loadedAccount&p=1'; // Trainer pass for everyone!
    }
    return 'Result=Failure&Reason=NotFound';
}


function load_story($email, $pass): string
{
    $story = get_story($email, $pass);
    if ($story)
    {
        $result = "Result=Success&";
        $result .= 'extra=' . encode_story($story);
        $result .= '&dw=' . date('w');
        for ($gen = 1; $gen <= 6; $gen++)
        {
            $result .= "&dextra$gen=" . $story["pokedex_$gen"];
            $result .= "&dcextra$gen=" . convertIntToString(create_Check_Sum($story["pokedex_$gen"]));
        }
        for ($i = 1; $i <= 3; $i++)
        {
            if (array_key_exists("profile{$i}", $story))
            {
                $profile = $story["profile{$i}"];
                $result .= "&Nickname{$i}={$profile['Nickname']}&";
                $result .= "Version{$i}={$profile['Color']}";
            }
        }
        return $result;
    }
    return 'Result=Success&extra=ycm';
}

function load_story_profile($email, $pass): string
{
    $result = '';
    $whichProfile = $_POST['whichProfile'];
    $profile = get_story_profile($email, $whichProfile);
    if ($profile)
    {
        $encoded_data = encode_story_profile($profile);
        $result .= "CS={$profile['CurrentSave']}&";
        $result .= "CT={$profile['CurrentTime']}&";
        $result .= "Gender={$profile['Gender']}&";
        $result .= "extra={$encoded_data[0]}&";  // Items
        $result .= "extra2={$encoded_data[1]}&";
        $result .= "extra3={$encoded_data[2][0]}";  // Pokemons data
        $result .= $encoded_data[2][1];  // Pokemon Nicknames
        $result .= "&extra4={$encoded_data[3]}&";
        $result .= "extra5={$encoded_data[4]}";
        $result = 'Result=Success&' . $result;
        return $result;
    }
    return 'Result=Failure&Reason=NotFound';
}

function save_story($email, $pass): string
{
    $new_data = array();
    $whichProfile = $_POST['whichProfile'];
    parse_str($_POST['extra'], $save_info);


    if(isset($save_info['NewGameSave']))
    {
        $new_data['story']["profile{$whichProfile}"] = ['Nickname' => $save_info['Nickname'],
            'Color' => $save_info["Color"],
            'Gender' => $save_info["Gender"],
            'Money' => 10,
            'CurrentTime' => 100];
    }

    if(isset($save_info['MapSave']))
    {
        $new_data['story']["profile{$whichProfile}"]['MapLoc'] = $save_info['MapLoc'];
        $new_data['story']["profile{$whichProfile}"]['MapSpot'] = $save_info['MapSpot'];
    }

    if(isset($save_info['MSave']))
    {
        $new_data['story']["profile{$whichProfile}"]['Money'] = convertStringToInt($save_info['MA']);
    }

    if(isset($save_info['CS']))
    {
        $new_data['story']["profile{$whichProfile}"]['CurrentSave'] = $save_info['CS'];
    }

    if(isset($save_info['TimeSave']))
    {
        $new_data['story']["profile{$whichProfile}"]['CurrentTime'] = $save_info['CT'];
    }

    if(isset($_POST['needD']))
    {
        for ($gen = 1; $gen <= 6; $gen++)
        {
            $new_data['story']["pokedex_{$gen}"] = $_POST["dextra{$gen}"];
        }
    }

    $pokes = decode_pokeinfo($_POST['extra3'], $email);
    foreach ($pokes as $key => $poke)
    {
        if (isset($poke['needNickname']))
        {
            $pokenicknum = $poke['needNickname'];
            $nickname = $save_info["PokeNick{$pokenicknum}"];
            $pokes[$key]['Nickname'] = $nickname;
            unset($pokes[$key]['needNickname']);
        }
        if (isset($poke['needSaveID']))
        {
            echo "PID{$poke['needSaveID']}={$poke['saveID']}&";
            unset($poke['needSaveID']);
        }
    }

    $items = decode_inventory($_POST['extra4']);
    $extra = decode_extra($_POST['extra2']);

    $new_data['story']["profile{$_POST["whichProfile"]}"]['poke'] = $pokes;
    $new_data['story']["profile{$_POST["whichProfile"]}"]['extra'] = $extra;
    $new_data['story']["profile{$_POST["whichProfile"]}"]['items'] = $items;
    if (update_account_data($email, $pass, $new_data))
    {
        return 'Result=Success';
    }
    return 'Result=Failure&Reason=NotFound';
}

function delete_story(string $email, string $pass): string
{
    $whichProfile = $_POST['whichProfile'];
    if (delete_profile($email, $pass, 'story', "profile{$whichProfile}")) {
        return 'Result=Success';
    }
    return 'Result=Failure&Reason=NotFound';
}

function load_1v1($email, $pass)
{
    $profiles = get_1v1($email);
    if (isset($profiles))
    {
        $encoded_data = encode_1v1($profiles);
        return "Result=Success&extra=$encoded_data&extra2=yqym";
    }
    return 'Result=Success&extra=ycm&extra2=yqym';
}

function save_1v1($email, $pass): string
{
    $whichProfile = $_POST['whichProfile'];
    $new_data['1v1']["profile{$whichProfile}"] = decode_1v1($_POST['extra']);
    if (update_account_data($email, $pass, $new_data))
    {
        return 'Result=Success&Reason=loadedAccount';
    }
    return 'Result=Failure&Reason=NotFound';
}

function delete_1v1(string $email, string $pass): string
{
    $whichProfile = $_POST['whichProfile'];
    delete_profile($email, $pass, '1v1', "profile{$whichProfile}");
    return 'Result=Success';
}

function maintenance()
{
    echo 'Result=Failure&Reason=maintenance';
    exit();
}

$email = $_POST['Email'];
$pass = $_POST['Pass'];

switch ($_POST['Action'])
{
case 'createAccount':
    echo create_account($email, $pass);
    break;
case 'loadAccount':
    echo load_account($email, $pass);
    break;
case 'loadStory':
    echo load_story($email, $pass);
    break;
case 'loadStoryProfile':
    echo load_story_profile($email, $pass);
    break;
case 'deleteStory':
    echo delete_story($email, $pass);
    break;
case 'saveStory':
    echo save_story($email, $pass);
    break;
case 'load1on1':
    echo load_1v1($email, $pass);
    break;
case 'save1on1':
    echo save_1v1($email, $pass);
    break;
case 'delete1on1':
    echo delete_1v1($email, $pass);
    break;
}
?>
